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

package com.mw.beam.beamwallet.screens.address_details

import com.mw.beam.beamwallet.base_screen.BasePresenter
import com.mw.beam.beamwallet.core.entities.TxDescription
import com.mw.beam.beamwallet.core.helpers.ChangeAction
import io.reactivex.disposables.Disposable

/**
 * Created by vain onnellinen on 3/4/19.
 */
class AddressPresenter(currentView: AddressContract.View, currentRepository: AddressContract.Repository, private val state: AddressState)
    : BasePresenter<AddressContract.View, AddressContract.Repository>(currentView, currentRepository),
        AddressContract.Presenter {
    private lateinit var txStatusSubscription: Disposable

    override fun onViewCreated() {
        super.onViewCreated()
        state.address = view?.getAddress()
        view?.init(state.address ?: return)
    }

    override fun onShowQR() {

    }

    override fun onEditAddress() {

    }

    override fun onDeleteAddress() {
        repository.deleteAddress()
        view?.finishScreen()
    }

    override fun onTransactionPressed(txDescription: TxDescription) {
        view?.showTransactionDetails(txDescription)
    }

    override fun initSubscriptions() {
        super.initSubscriptions()

        txStatusSubscription = repository.getTxStatus().subscribe { data ->
            view?.configTransactions(
                    when (data.action) {
                        ChangeAction.REMOVED -> state.deleteTransaction(data.tx)
                        else -> state.updateTransactions(data.tx)
                    })
        }
    }

    override fun getSubscriptions(): Array<Disposable>? = arrayOf(txStatusSubscription)
}
