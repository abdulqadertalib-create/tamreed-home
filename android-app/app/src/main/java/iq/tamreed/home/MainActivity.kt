package iq.tamreed.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider

private val Purple = Color(0xFF5B3FA5)
private val PurpleDark = Color(0xFF3F287C)
private val PurpleSoft = Color(0xFFF1EDFA)
private val Background = Color(0xFFF8F7FB)
private val TextDark = Color(0xFF202124)
private val TextGray = Color(0xFF73717A)
private val Green = Color(0xFF159B63)

private data class Service(
    val name: String,
    val description: String,
    val price: String,
    val icon: String
)

private val services = listOf(
    Service("تمريض منزلي", "زيارة ممرض أو ممرضة إلى منزلك", "25,000 دينار", "♥"),
    Service("قياس الضغط والسكر", "قياس العلامات الحيوية في المنزل", "10,000 دينار", "＋"),
    Service("تغيير الضماد", "تنظيف وتغيير الضمادات", "15,000 دينار", "✚"),
    Service("إعطاء إبرة", "إعطاء الحقن حسب وصفة الطبيب", "10,000 دينار", "●")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TamreedApp() }
    }
}

private enum class AppStage { PHONE, OTP, LOCATION, MAIN }

@Composable
fun TamreedApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val prefs = remember {
            context.getSharedPreferences("tamreed", Context.MODE_PRIVATE)
        }

        var stage by remember {
            mutableStateOf(
                if (prefs.getBoolean("logged_in", false)) AppStage.MAIN
                else AppStage.PHONE
            )
        }
        var phone by remember { mutableStateOf("") }
        var selectedTab by remember { mutableIntStateOf(0) }
        var pendingService by remember { mutableStateOf<Service?>(null) }
        var locationText by remember {
            mutableStateOf(
                prefs.getString("location", "لم يتم تحديد الموقع")
                    ?: "لم يتم تحديد الموقع"
            )
        }

        when (stage) {
            AppStage.PHONE -> PhoneScreen(
                phone = phone,
                onPhoneChange = { phone = it.filter(Char::isDigit).take(11) },
                onContinue = {
                    if (phone.length >= 10) stage = AppStage.OTP
                }
            )

            AppStage.OTP -> OtpScreen(
                phone = phone,
                onBack = { stage = AppStage.PHONE },
                onVerified = { stage = AppStage.LOCATION }
            )

            AppStage.LOCATION -> LocationScreen(
                locationText = locationText,
                onLocation = { text ->
                    locationText = text
                    prefs.edit().putString("location", text).apply()
                },
                onContinue = {
                    prefs.edit().putBoolean("logged_in", true).apply()
                    stage = AppStage.MAIN
                }
            )

            AppStage.MAIN -> Scaffold(
                containerColor = Background,
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("الرئيسية") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Search, null) },
                            label = { Text("طلباتي") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.AccountCircle, null) },
                            label = { Text("حسابي") }
                        )
                    }
                }
            ) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        location = locationText,
                        onService = { pendingService = it }
                    )
                    1 -> OrdersScreen(Modifier.padding(padding))
                    else -> ProfileScreen(Modifier.padding(padding)) {
                        prefs.edit().clear().apply()
                        stage = AppStage.PHONE
                    }
                }
            }
        }

        pendingService?.let { service ->
            RequestDialog(
                service = service,
                location = locationText,
                onDismiss = { pendingService = null }
            )
        }
    }
}

@Composable
private fun PhoneScreen(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(55.dp))
            Logo()
            Spacer(Modifier.height(24.dp))

            Text(
                "أهلاً بك في التمريض المنزلي",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "رعاية تمريضية موثوقة تصل إلى باب منزلك في الفلوجة",
                color = TextGray,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(35.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("تسجيل الدخول", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("سنرسل رمز تحقق إلى رقم هاتفك", color = TextGray)
                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رقم الهاتف العراقي") },
                        placeholder = { Text("07XXXXXXXXX") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(Purple)
                    ) {
                        Text("متابعة", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("بياناتك محفوظة بأمان", color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OtpScreen(
    phone: String,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    var otp by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "رجوع")
                }
                Text(
                    "التحقق من الرقم",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(45.dp))

            Box(
                Modifier.size(82.dp).clip(CircleShape).background(PurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Phone,
                    null,
                    tint = Purple,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            Text("أدخل رمز التحقق", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("تم إرسال الرمز إلى $phone", color = TextGray)
            Spacer(Modifier.height(25.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = {
                    otp = it.filter(Char::isDigit).take(6)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("رمز OTP") },
                placeholder = { Text("123456") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "للاختبار الحالي استخدم: 123456",
                color = TextGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onVerified,
                enabled = otp.length == 6,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(Purple)
            ) {
                Text("تأكيد الرقم", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = {}) {
                Text("إعادة إرسال الرمز")
            }
        }
    }
}

@Composable
private fun LocationScreen(
    locationText: String,
    onLocation: (String) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            val manager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> null
            }

            val hasPermission =
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

            val loc =
                if (hasPermission && provider != null) {
                    manager.getLastKnownLocation(provider)
                } else null

            onLocation(
                if (loc != null) "موقعك الحالي محدد ✓"
                else "الفلوجة، الأنبار"
            )
        }

        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(55.dp))

            Box(
                Modifier.size(96.dp).clip(CircleShape).background(PurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = Purple,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("حدد موقعك", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "نحتاج موقعك لإرسال أقرب ممرض إلى منزلك",
                color = TextGray,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(30.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "الموقع الحالي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(locationText, color = TextGray)
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            loading = true
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(Purple)
                    ) {
                        Icon(Icons.Default.LocationOn, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (loading) "جارٍ تحديد الموقع..."
                            else "تحديد موقعي تلقائياً"
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(PurpleDark)
            ) {
                Text(
                    "متابعة إلى التطبيق",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "يمكنك تغيير الموقع لاحقاً",
                color = TextGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Logo() {
    Box(
        Modifier.size(82.dp).clip(RoundedCornerShape(26.dp)).background(Purple),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MedicalServices,
            null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    location: String,
    onService: (Service) -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(Purple)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "رعايتك تبدأ من هنا",
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "ممرضون موثوقون لخدمتك في منزلك",
                                color = Color.White.copy(alpha = .9f)
                            )
                        }

                        Box(
                            Modifier.size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = .16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = .12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            location,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "الخدمات",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                "اختر الخدمة التي تحتاجها",
                color = TextGray,
                fontSize = 13.sp
            )
        }

        items(services) { service ->
            ServiceCard(service, onService)
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ServiceCard(
    service: Service,
    onService: (Service) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(PurpleSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        service.icon,
                        color = Purple,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        service.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        service.description,
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }

                Icon(
                    Icons.Default.Verified,
                    null,
                    tint = Green,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("السعر التقريبي", color = TextGray, fontSize = 12.sp)
                    Text(
                        service.price,
                        color = PurpleDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { onService(service) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(Purple)
                ) {
                    Text("طلب الخدمة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RequestDialog(
    service: Service,
    location: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تأكيد طلب الخدمة", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    service.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("السعر: ${service.price}", color = PurpleDark)
                Spacer(Modifier.height(6.dp))
                Text("الموقع: $location", color = TextGray)
                Spacer(Modifier.height(10.dp))
                Text(
                    "سيتم تجهيز الطلب وإرساله للممرض المتاح.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(Purple)
            ) {
                Text("تأكيد الطلب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun OrdersScreen(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().background(Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(45.dp))

        Box(
            Modifier.size(90.dp).clip(CircleShape).background(PurpleSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = Purple,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(22.dp))
        Text("طلباتي", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "ستظهر هنا جميع طلبات التمريض وحالتها",
            color = TextGray
        )

        Spacer(Modifier.height(24.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "لا توجد طلبات حالياً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "اختر خدمة من الصفحة الرئيسية لبدء طلب جديد",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    onLogout: () -> Unit
) {
    Column(
        modifier.fillMaxSize().background(Background).padding(20.dp)
    ) {
        Text("حسابي", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(62.dp).clip(CircleShape).background(PurpleSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            null,
                            tint = Purple,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "مستخدم التمريض المنزلي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "حساب العميل",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                ProfileRow(Icons.Default.LocationOn, "العنوان والموقع", "إدارة موقع المنزل")
                ProfileRow(Icons.Default.Settings, "الإعدادات", "إعدادات التطبيق")
                ProfileRow(
                    Icons.Default.Logout,
                    "تسجيل الخروج",
                    "الخروج من الحساب",
                    onLogout
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { action?.invoke() }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(PurpleSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Purple)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextGray, fontSize = 12.sp)
        }
    }
}
