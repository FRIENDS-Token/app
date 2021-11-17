package io.horizontalsystems.bankwallet.modules.coin.treasuries

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuriesData
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuryItem
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.reactivex.disposables.CompositeDisposable

class CoinTreasuriesViewModel(
    private val service: CoinTreasuriesService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val viewStateLiveData = MutableLiveData<ViewState>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val coinTreasuriesLiveData = MutableLiveData<CoinTreasuriesData>()
    val treasuryTypeSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        service.stateObservable
            .subscribeIO({ state ->
                loadingLiveData.postValue(state == DataState.Loading)

                when (state) {
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        syncCoinTreasuriesData(state.data)
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error)
                    }
                }
            }, {
                viewStateLiveData.postValue(ViewState.Error)
            }).let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun onToggleSortType() {
        service.sortDescending = !service.sortDescending
    }

    fun onClickTreasuryTypeSelector() {
        treasuryTypeSelectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.treasuryType, service.treasuryTypes))
        )
    }

    fun onSelectTreasuryType(type: TreasuryTypeFilter) {
        service.treasuryType = type
        treasuryTypeSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun onTreasuryTypeSelectorDialogDismiss() {
        treasuryTypeSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    private fun syncCoinTreasuriesData(coinTreasuries: List<CoinTreasury>) {
        val coinTreasuriesData = CoinTreasuriesData(
            Select(service.treasuryType, service.treasuryTypes),
            service.sortDescending,
            coinTreasuries.map {
                coinTreasuryItem(it)
            }
        )
        coinTreasuriesLiveData.postValue(coinTreasuriesData)
    }

    private fun coinTreasuryItem(coinTreasury: CoinTreasury) =
        CoinTreasuryItem(
            fund = coinTreasury.fund,
            fundLogoUrl = "https://markets.nyc3.digitaloceanspaces.com/fund-icons/ios/${coinTreasury.fundUid}@3x.png",
            country = coinTreasury.countryCode,
            amount = numberFormatter.formatCoinValueAsShortened(coinTreasury.amount, service.coin.code),
            amountInCurrency = numberFormatter.formatCurrencyValueAsShortened(
                CurrencyValue(
                    service.currency,
                    coinTreasury.amountInCurrency
                )
            )
        )
}
