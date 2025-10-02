package ai.amani.biomatchdemo

import ai.amani.base.utility.AmaniVersion
import ai.amani.biomatch.sdk.AmaniBioMatchSDK
import ai.amani.biomatchdemo.MainActivity
import ai.amani.sdk.Amani
import android.app.Application

class App :Application(){

    override fun onCreate() {
        super.onCreate()
        // Configure KYC SDK
        Amani.configure(
            context = this,
            server = PasswordProperties.SERVER_URL_KYC,
            enabledFeatures = listOf(),
            version = AmaniVersion.V2
        )

        // Configure BioMatch SDK
        AmaniBioMatchSDK.configure(
            context = this,
            server = PasswordProperties.SERVER_URL,
            token = PasswordProperties.TOKEN,
        )
    }
}