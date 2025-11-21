package ai.amani.biomatchdemo

import ai.amani.base.utility.AmaniVersion
import ai.amani.biomatch.sdk.AmaniBioMatchSDK
import ai.amani.biomatch.sdk.features.pin.callback.PINCallback
import ai.amani.biomatch.sdk.features.pin.presentation.PinScreenConfig
import ai.amani.biomatch.sdk.features.selfie.callback.SelfieCallback
import ai.amani.biomatch.sdk.features.selfie.presentation.SelfieCaptureConfig
import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.interfaces.IFragmentCallBack
import ai.amani.sdk.interfaces.ILoginCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        
        setContent {
            MaterialTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val loading = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }
    val alertMessage = remember { mutableStateOf("") }
    val alertTitle = remember { mutableStateOf("") }
    val isSuccess = remember { mutableStateOf(false) }

    LaunchedEffect("login") {
        loading.value = true
        Amani.sharedInstance().startSession(
            id = "",
            lang = "eng",
            token = PasswordProperties.TOKEN,
            callback = object : ILoginCallBack {
                override fun cb(success: Boolean) {
                    CoroutineScope(Dispatchers.Main).launch {
                        loading.value = false
                        if (!success) {
                            isSuccess.value = false
                            alertTitle.value = "Hata"
                            alertMessage.value = "Login başarısız oldu!"
                            showAlert.value = true
                        }
                    }
                }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                onKYCClick = { CoroutineScope(Dispatchers.Main).launch {
                    navController.navigate("kyc_selfie")
                } },
                onPaymentTabletClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        navController.navigate("payment_selfie")
                    }
                }
            )
        }

        composable("kyc_selfie") {
            KYCSelfieScreen(
                onNavigateToPin = {
                    CoroutineScope(Dispatchers.Main).launch {
                        navController.navigate("pin/kyc")
                    }},
                onBack = { navController.popBackStack() },
                onRejected = {
                    navController.popBackStack()
                    isSuccess.value = false
                    alertTitle.value = "Hata"
                    alertMessage.value = "Selfie reddedildi!"
                    showAlert.value = true
                },
                loaderState = { loading.value = it }
            )
        }

        composable("payment_selfie") {
            PaymentSelfieScreen(
                onSelfieCaptured = {
                    CoroutineScope(Dispatchers.Main).launch {
                        navController.navigate("pin/payment")
                    }},
                onBack = { navController.popBackStack() }
            )
        }

        composable("pin/{flow}") { backStackEntry ->
            val flow = backStackEntry.arguments?.getString("flow") ?: ""
            PINScreen(
                flow = flow,
                onPinEntered = { pin ->
                    loading.value = true
                    when (flow) {
                        "kyc" -> {
                            AmaniBioMatchSDK.PIN().enable(
                                pin,
                                callback = object : PINCallback {
                                    override fun onSuccess() {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(1000)
                                            loading.value = false
                                            isSuccess.value = true
                                            alertTitle.value = "Success"
                                            alertMessage.value = "KYC process has been successfully completed!"
                                            showAlert.value = true
                                        }
                                    }

                                    override fun onError(exception: Exception) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(1000)
                                            loading.value = false
                                            isSuccess.value = false
                                            alertTitle.value = "Error"
                                            alertMessage.value = "KYC failed!"
                                            showAlert.value = true
                                        }
                                    }
                                }
                            )
                        }

                        "payment" -> {
                            AmaniBioMatchSDK.Selfie().upload(
                                pin = pin,
                                callback = object : SelfieCallback {
                                    override fun onSuccess(profileID: String, documentID: String) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(1000)
                                            loading.value = false
                                            isSuccess.value = true
                                            alertTitle.value = "Success"
                                            alertMessage.value = "Payment is done!" +
                                                    " With profile ID: $profileID"
                                            showAlert.value = true
                                        }
                                    }

                                    override fun onError(exception: Exception) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(1000)
                                            loading.value = false
                                            isSuccess.value = false
                                            alertTitle.value = "Error"
                                            alertMessage.value = "Payment process has been failed! + " +
                                                    "exception ${exception.message} + " +
                                                    " ${exception.localizedMessage}"
                                            showAlert.value = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    // ✅ Loading göstergesi
    if (loading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    // ✅ Alert Dialog (ikon ortada büyük)
    if (showAlert.value) {
        AlertDialog(
            onDismissRequest = {
                showAlert.value = false
                CoroutineScope(Dispatchers.Main).launch {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSuccess.value) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50), // yeşil
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = alertTitle.value, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = alertMessage.value, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlert.value = false
                        CoroutineScope(Dispatchers.Main).launch {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PINScreen(flow: String, onPinEntered: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            title = { Text("PIN Entry") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        val fragment = AmaniBioMatchSDK.PIN().start(
            config = PinScreenConfig(
                pinLength = 4,
                title = "Enter PIN",
                subtitle = "Please enter your security PIN",
                backgroundColor = Color.White,
                titleColor = Color(0xFF1F2937),
                subtitleColor = Color(0xFF6B7280),
                pinDotFilledColor = Color(0xFF3B82F6),
                pinDotEmptyColor = Color(0xFFE5E7EB),
                keypadButtonBackgroundColor = Color(0xFFF9FAFB),
                keypadButtonNumberColor = Color(0xFF1F2937),
                deleteButtonContentColor = Color(0xFFEF4444),
                keypadButtonBorderColor = Color(0xFFE5E7EB)
            ),
            onPinComplete = { pin -> onPinEntered(pin) }
        )

        FragmentInCompose(fragment!!)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSelfieScreen(onSelfieCaptured: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Selfie for Payment") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        val fragment = AmaniBioMatchSDK.Selfie().start(
            config = SelfieCaptureConfig(
                autoSelfieEnabled = true,
                manualCaptureButtonTimeOut = 34000,
                manualCaptureButtonInnerColor = Color.Blue,
                manualCaptureButtonOuterColor = Color.Blue,
                manualCaptureButtonLoaderColor = Color.Blue
            ),
            onCaptureCompleted = { filePath ->
                onSelfieCaptured("filePath")
            }
        )

        FragmentInCompose(fragment)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KYCSelfieScreen(
    onNavigateToPin: () -> Unit, onBack: () -> Unit, onRejected: () -> Unit, loaderState: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Selfie (For KYC)") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        val context = LocalContext.current
        val selfieFragment = Amani.sharedInstance().Selfie().start(
            docType = "XXX_SE_0",
            callBack = object : IFragmentCallBack {
                override fun cb(p0: Bitmap?, p1: Boolean?, p2: File?) {
                    loaderState.invoke(true)
                    Amani.Selfie().upload(context as FragmentActivity, "XXX_SE_0") {

                    }

                    Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack {
                        override fun onError(
                            type: String?,
                            error: ArrayList<AmaniError?>?
                        ) {
                        }

                        override fun profileStatus(profileStatus: ProfileStatus) {
                        }

                        override fun stepsResult(stepsResult: StepsResult?) {
                            stepsResult?.result?.forEach {
                                if (it.title == "Selfie") {
                                    when(it.status) {
                                        "APPROVED" -> {
                                            onNavigateToPin.invoke()
                                            Amani.sharedInstance().AmaniEvent().removeListener()
                                            loaderState.invoke(false)
                                        }

                                        "PROCESSING" -> {

                                        }

                                        else -> {
                                            onRejected.invoke()
                                            Amani.sharedInstance().AmaniEvent().removeListener()
                                            loaderState.invoke(false)
                                        }

                                    }
                                }
                            }
                        }
                    })
                }
            }
        )

        FragmentInCompose(fragment = selfieFragment!!)
    }
}

@Composable
fun HomeScreen(
    onKYCClick: () -> Unit,
    onPaymentTabletClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Button(
                onClick = onKYCClick,
                modifier = Modifier
                    .width(280.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "KYC + Payment Register Flow",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onPaymentTabletClick,
                modifier = Modifier
                    .width(280.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Payment Via Tablet Flow",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FragmentInCompose(fragment: Fragment) {
    val context = LocalContext.current
    val fragmentManager = (context as AppCompatActivity).supportFragmentManager

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { containerView ->
            val existingFragment = fragmentManager.findFragmentById(containerView.id)
            if (existingFragment == null || existingFragment::class != fragment::class) {
                fragmentManager.beginTransaction()
                    .replace(containerView.id, fragment)
                    .commit()
            }
        }
    )
}