package com.deenislam.sdk.service.network.response.zakat

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Data(
    val AgricultureAmount: Double,
    val BusinessPayment: Double,
    val CarPayment: Double,
    val CashInBankAccount: Double,
    val CashInHands: Double,
    val CashinBusiness: Double,
    val CreditCardPayment: Double,
    val DebtsAndLiabilities: Double,
    val DebtsToFamily: Double,
    val DebtsToOthers: Double,
    val EntryDate: String,
    val GoldEquivalentamount: Double,
    val HomePayment: Double,
    val HouseRent: Double,
    val Id: Int,
    val InvestmentStockMarket: Double,
    val MSISDN: String,
    val Nisab: Double,
    val OtherInvestments: Double,
    val OthercapitalAmount: Double,
    val PensionAmount: Double,
    val ProductinBusiness: Double,
    val PropertyValue: Double,
    val SilverEquivalentamount: Double,
    val TotalAssets: Double,
    val ZakatPayable: Double,
    val isactive: Boolean,
    val language: String
) :Parcelable