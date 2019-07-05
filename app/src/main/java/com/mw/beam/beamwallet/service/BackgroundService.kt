/*
 * // Copyright 2018 Beam Development
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //    http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 */

package com.mw.beam.beamwallet.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mw.beam.beamwallet.R
import com.mw.beam.beamwallet.core.App
import com.mw.beam.beamwallet.core.entities.OnTxStatusData
import com.mw.beam.beamwallet.core.helpers.ChangeAction
import com.mw.beam.beamwallet.core.helpers.Status
import com.mw.beam.beamwallet.core.helpers.TxSender
import com.mw.beam.beamwallet.screens.app_activity.AppActivity
import io.reactivex.disposables.Disposable

class BackgroundService : JobService() {
    private val repository = BackgroundServiceRepository()

    companion object {
        private var txDisposable: Disposable? = null
        private const val CHANNEL_ID = "com.mw.beam.beamwallet.service.BackgroundService"
        private const val REQUEST_CODE = 86131

        private fun createNotificationChannel(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.notification_channel_name)
                val descriptionText = context.getString(R.string.notification_channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun receiveOnTxData(data: OnTxStatusData) {
            App.self.apply {
                if (data.action == ChangeAction.ADDED && App.showNotification) {
                    createNotificationChannel(this)

                    val txDescription = data.tx?.firstOrNull()

                    if (txDescription != null && txDescription.sender == TxSender.RECEIVED) {
                        val txId = txDescription.id

                        val intent = Intent(applicationContext, AppActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(AppActivity.TRANSACTION_ID, txId)
                        }

                        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                                .setSmallIcon(R.drawable.logo)
                                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
                                .setContentTitle(getString(R.string.notification_receive_content_title))
                                .setContentText(getString(R.string.notification_receive_content_text))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .setVibrate(longArrayOf(250, 250, 250, 250))
                                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                .setLights(getColor(R.color.received_color), 2000, 2000)
                                .setContentIntent(PendingIntent.getActivity(applicationContext, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                                .build()

                        with(NotificationManagerCompat.from(applicationContext)) {
                            notify(txId.hashCode(), notification)
                        }
                    }
                }
            }
        }
    }


    override fun onStartJob(params: JobParameters?): Boolean {
        val password = repository.getPassword()

        if (repository.isWalletRunning() || password.isNullOrBlank()) {
            return false
        }

        val status = repository.openWallet(password)

        if (status != Status.STATUS_OK || txDisposable != null) {
            return false
        }


        txDisposable = repository.getTxStatus().subscribe {
            receiveOnTxData(it)
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        if (!App.isAppRunning) {
            repository.closeWallet()
            txDisposable?.dispose()
            Process.killProcess(Process.myPid())
        }
        return false
    }
}
