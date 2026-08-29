package iq.tamreed.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private const val SUPABASE_URL = "https://vpnqznfwnqlsztwlcepr.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_Zj_mqQ7rVoD_lAqiPR7BVw_MnxzURwZ"
private val Primary = Color(0xFF087F8C)
private val Background = Color(0xFFF5FAFA)
private val TextDark = Color(0xFF173238)
private val TextGray = Color(0xFF718083)
private val Green = Color(0xFF159B63)
private val Red = Color(0xFFD32F2F)

data class Service(val id:String,val name:String,val description:String,val price:Int)
data class Order(val id:String,val service:String,val address:String,val notes:String,val lat:Double?,val lon:Double?,val status:String,val createdAt:String)
data class ApiResult(val success:Boolean,val message:String,val token:String?=null,val userId:String?=null)
data class LocationData(val lat:Double,val lon:Double,val address:String)

class MainActivity : ComponentActivity() {
    private var location by mutableStateOf<LocationData?>(null)
    private var locationLoading by mutableStateOf(false)
    private var locationError by mutableStateOf("")
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.ACCESS_FINE_LOCATION] == true || r[Manifest.permission.ACCESS_COARSE_LOCATION] == true) loadLocation()
        else locationError = "يجب السماح للتطبيق بالوصول إلى موقعك"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary=Primary,background=Background,surface=Color.White)) {
                    TamreedApp(
                        location=location,
                        locationLoading=locationLoading,
                        locationError=locationError,
                        requestLocation={ requestLocation() }
                    )
                }
            }
        }
    }
    private fun requestLocation() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) loadLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    @SuppressLint("MissingPermission") private fun loadLocation() {
        locationLoading=true; locationError=""
        val manager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val provider=when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER)->LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)->LocationManager.NETWORK_PROVIDER
                else->null
            }
            val last=provider?.let { manager.getLastKnownLocation(it) }
            if(last==null){ locationLoading=false; locationError="فعّل GPS ثم اضغط تحديد موقعي مرة أخرى"; return }
            val lat=last.latitude; val lon=last.longitude
            Thread {
                val address=try {
                    @Suppress("DEPRECATION") val list=Geocoder(this,Locale("ar","IQ")).getFromLocation(lat,lon,1)
                    list?.firstOrNull()?.getAddressLine(0) ?: "الموقع الحالي"
                } catch(_:Exception){ "الموقع الحالي" }
                runOnUiThread { location=LocationData(lat,lon,address); locationLoading=false }
            }.start()
        } catch(e:Exception){ locationLoading=false; locationError="تعذر تحديد الموقع: ${e.message ?: "خطأ"}" }
    }
}

@Composable fun TamreedApp(location:LocationData?,locationLoading:Boolean,locationError:String,requestLocation:()->Unit){
    val context=LocalContext.current
    val prefs=remember{context.getSharedPreferences("tamreed_home",Context.MODE_PRIVATE)}
    val scope=rememberCoroutineScope()
    var screen by remember { mutableStateOf(if(prefs.getBoolean("logged_in",false)) "home" else "phone") }
    var phone by remember { mutableStateOf(prefs.getString("phone","") ?: "") }
    var otp by remember{mutableStateOf("")}; var loading by remember{mutableStateOf(false)}; var error by remember{mutableStateOf("")}
    var services by remember{mutableStateOf<List<Service>>(emptyList())}; var orders by remember{mutableStateOf<List<Order>>(emptyList())}
    var selected by remember{mutableStateOf<Service?>(null)}; var address by remember{mutableStateOf(prefs.getString("address","") ?: "")}; var notes by remember{mutableStateOf("")}
    fun refreshServices(){ scope.launch { services=fetchServices(prefs.getString("access_token","") ?: "") } }
    fun refreshOrders(){ scope.launch { orders=fetchOrders(prefs.getString("access_token","") ?: "",prefs.getString("user_id","") ?: "") } }
    LaunchedEffect(screen){ if(screen=="home"||screen=="services") refreshServices(); if(screen=="orders") refreshOrders() }
    when(screen){
        "phone"->PhoneScreen(phone,loading,error,{phone=normalizeArabicDigits(it).filter(Char::isDigit).take(15);error=""}){
            val p=formatIraqiPhone(phone); if(p==null){error="أدخل رقم هاتف عراقي صحيح مثل 07812345678";return@PhoneScreen}; loading=true; scope.launch{val r=sendOtp(p);loading=false;if(r.success){phone=p;screen="otp"}else error=r.message}
        }
                "otp"->OtpScreen(phone,otp,loading,error,{otp=normalizeArabicDigits(it).filter(Char::isDigit).take(6);error=""},{screen="phone"}){
            if(otp.length!=6){error="أدخل رمز التحقق المكون من 6 أرقام";return@OtpScreen}
            loading=true; scope.launch { val r=verifyOtp(phone,otp); loading=false; if(r.success){
                prefs.edit().putBoolean("logged_in",true).putString("phone",phone).putString("access_token",r.token ?: "").putString("user_id",r.userId ?: "").apply(); screen="profile"
            } else error=r.message }
        }
        "profile"->ProfileScreen(phone,loading,error){name->loading=true;scope.launch{val r=saveProfile(prefs.getString("access_token","") ?: "",prefs.getString("user_id","") ?: "",name,phone);loading=false;if(r.success)screen="location" else error=r.message}}
        "location"->LocationScreen(location,locationLoading,locationError,requestLocation,{screen="home"})
        "home"->HomeScreen(phone,location,orders.size,{screen="services"},{screen="orders"},{screen="location";requestLocation()})
        "services"->ServicesScreen(services,{screen="home"}){selected=it;screen="request"}
        "request"->RequestScreen(selected,address,notes,location,error,loading,{address=it},{notes=it},{screen="services"}){
            val s=selected ?: return@RequestScreen
            if(address.isBlank()){error="اكتب عنوان المنزل أو استخدم الموقع المحدد";return@RequestScreen}
            if(location==null){error="حدد موقع المنزل قبل إرسال الطلب";return@RequestScreen}
            loading=true;scope.launch{val r=createBooking(prefs.getString("access_token","") ?: "",prefs.getString("user_id","") ?: "",s,address,notes,location);loading=false;if(r.success){prefs.edit().putString("address",address).apply();notes="";error="";refreshOrders();screen="success"}else error=r.message}
        }
        "success"->SuccessScreen({screen="home"},{screen="orders"})
        "orders"->OrdersScreen(orders,{screen="home"})
    }
}

@Composable fun PhoneScreen(phone:String,loading:Boolean,error:String,onChange:(String)->Unit,onSend:()->Unit){SimplePage({Icon(Icons.Default.Phone,null,tint=Primary,modifier=Modifier.size(70.dp))},"التمريض المنزلي","رعاية صحية موثوقة تصل إلى منزلك"){Card(shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(22.dp)){Text("تسجيل الدخول",fontSize=23.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("سجّل برقم هاتفك وسنرسل رمز تحقق SMS",color=TextGray);Spacer(Modifier.height(20.dp));OutlinedTextField(phone,onChange,Modifier.fillMaxWidth(),label={Text("رقم الهاتف")},placeholder={Text("07812345678")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),singleLine=true);ErrorText(error);Spacer(Modifier.height(16.dp));PrimaryButton("إرسال رمز التحقق",loading,onSend,Icons.Default.Sms)}}}}

@Composable fun OtpScreen(phone:String,otp:String,loading:Boolean,error:String,onChange:(String)->Unit,onBack:()->Unit,onVerify:()->Unit){Scaffold(topBar={TopAppBar(title={Text("تأكيد رقم الهاتف")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,"رجوع")}})}){p->Column(Modifier.fillMaxSize().background(Background).padding(p).padding(22.dp)){Spacer(Modifier.height(25.dp));Text("أدخل رمز التحقق",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("تم إرسال الرمز إلى $phone",color=TextGray);Spacer(Modifier.height(25.dp));OutlinedTextField(otp,onChange,Modifier.fillMaxWidth(),label={Text("رمز SMS")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true);ErrorText(error);Spacer(Modifier.height(18.dp));PrimaryButton("تحقق ودخول",loading,onVerify,Icons.Default.VerifiedUser)}}}

@Composable fun ProfileScreen(phone:String,loading:Boolean,error:String,onSave:(String)->Unit){var name by remember{mutableStateOf("")};SimplePage({Icon(Icons.Default.Person,null,tint=Primary,modifier=Modifier.size(65.dp))},"بياناتك الأساسية","أكمل ملفك مرة واحدة لتسهيل طلباتك"){OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("الاسم الكامل")},singleLine=true);Spacer(Modifier.height(12.dp));OutlinedTextField(phone,{},Modifier.fillMaxWidth(),label={Text("رقم الهاتف")},enabled=false);ErrorText(error);Spacer(Modifier.height(16.dp));PrimaryButton("حفظ ومتابعة",loading,{if(name.trim().length>=3)onSave(name.trim())},Icons.Default.CheckCircle)}}

@Composable fun LocationScreen(location:LocationData?,loading:Boolean,error:String,onLocate:()->Unit,onContinue:()->Unit){SimplePage({Icon(Icons.Default.LocationOn,null,tint=Primary,modifier=Modifier.size(70.dp))},"حدد موقع المنزل","سنستخدم الموقع لإرسال الممرض إلى المكان الصحيح"){Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text(if(location==null)"لم يتم تحديد الموقع بعد" else "تم تحديد موقع المنزل",fontWeight=FontWeight.Bold,color=if(location==null)TextDark else Green);Spacer(Modifier.height(8.dp));Text(location?.address ?: "اضغط الزر لتحديد موقعك تلقائياً",color=TextGray);if(location!=null){Spacer(Modifier.height(8.dp));Text("${"%.6f".format(Locale.US,location.lat)}, ${"%.6f".format(Locale.US,location.lon)}",fontSize=12.sp,color=TextGray)}}};ErrorText(error);Spacer(Modifier.height(16.dp));PrimaryButton(if(loading)"جارٍ تحديد الموقع..." else "تحديد موقعي الحالي",loading,onLocate,Icons.Default.MyLocation);Spacer(Modifier.height(10.dp));Button(onContinue,Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp),enabled=location!=null){Text("متابعة")}}}

@Composable fun HomeScreen(phone:String,location:LocationData?,count:Int,onServices:()->Unit,onOrders:()->Unit,onLocation:()->Unit){Scaffold(bottomBar={NavigationBar{NavigationBarItem(true,{},icon={Icon(Icons.Default.Home,null)},label={Text("الرئيسية")});NavigationBarItem(false,onOrders,icon={Icon(Icons.Default.ReceiptLong,null)},label={Text("طلباتي")})}}){p->LazyColumn(Modifier.fillMaxSize().background(Background).padding(p).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Text("أهلاً بك 👋",fontSize=28.sp,fontWeight=FontWeight.Bold,color=TextDark);Text(phone,color=TextGray)};item{Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Primary)){Column(Modifier.padding(22.dp)){Text("رعاية صحية في منزلك",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold);Text("احجز خدمة تمريضية مع تحديد موقع المنزل بدقة",color=Color.White)}}};item{Button(onServices,Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.AddLocationAlt,null);Spacer(Modifier.width(8.dp));Text("احجز خدمة تمريضية")}};item{Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.LocationOn,null,tint=Primary);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("موقع المنزل",fontWeight=FontWeight.Bold);Text(location?.address ?: "لم يتم تحديد الموقع",color=TextGray,fontSize=13.sp)}};TextButton(onLocation){Text("تحديث")}}};item{Button(onOrders,Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(15.dp)){Text("طلباتي السابقة ($count)")}}}}}

@Composable fun ServicesScreen(services:List<Service>,onBack:()->Unit,onSelect:(Service)->Unit){Scaffold(topBar={TopAppBar(title={Text("الخدمات التمريضية")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,"رجوع")}})}){p->if(services.isEmpty()){Box(Modifier.fillMaxSize().background(Background).padding(p),contentAlignment=Alignment.Center){Text("لا توجد خدمات متاحة حالياً",color=TextGray)}}else LazyColumn(Modifier.fillMaxSize().background(Background).padding(p).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(services){s->Card(Modifier.fillMaxWidth().clickable{onSelect(s)},shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.MedicalServices,null,tint=Primary);Spacer(Modifier.width(12.dp));Text(s.name,fontSize=18.sp,fontWeight=FontWeight.Bold)};if(s.description.isNotBlank())Text(s.description,color=TextGray,modifier=Modifier.padding(top=7.dp));Text("${s.price} د.ع",color=Primary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))}}}}}}

@Composable fun RequestScreen(service:Service?,address:String,notes:String,location:LocationData?,error:String,loading:Boolean,onAddress:(String)->Unit,onNotes:(String)->Unit,onBack:()->Unit,onSubmit:()->Unit){Scaffold(topBar={TopAppBar(title={Text("تأكيد طلب الخدمة")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,"رجوع")}})}){p->LazyColumn(Modifier.fillMaxSize().background(Background).padding(p).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text(service?.name ?: "الخدمة",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Primary);Text("السعر: ${service?.price ?: 0} د.ع",color=TextGray)};item{OutlinedTextField(address,onAddress,Modifier.fillMaxWidth(),label={Text("عنوان المنزل")},placeholder={Text("الفلوجة - الحي - الشارع - رقم المنزل")},minLines=2)};item{OutlinedTextField(notes,onNotes,Modifier.fillMaxWidth(),label={Text("ملاحظات للممرض")},placeholder={Text("حالة المريض أو أي تعليمات مهمة")},minLines=3)};item{Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text("الموقع الجغرافي",fontWeight=FontWeight.Bold);Text(if(location==null)"غير محدد" else "تم تحديد الموقع بدقة ✔",color=if(location==null)Red else Green);if(location!=null)Text(location.address,color=TextGray,fontSize=13.sp)}}};item{ErrorText(error);PrimaryButton("إرسال طلب التمريض",loading,onSubmit,Icons.Default.Send)}}}}

@Composable fun SuccessScreen(onHome:()->Unit,onOrders:()->Unit){SimplePage({Icon(Icons.Default.CheckCircle,null,tint=Green,modifier=Modifier.size(85.dp))},"تم إرسال طلبك بنجاح","تم حفظ الطلب والموقع، وسيظهر في النظام مباشرة"){Button(onHome,Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(15.dp)){Text("العودة للرئيسية")};Spacer(Modifier.height(10.dp));Button(onOrders,Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(15.dp)){Text("عرض طلباتي")}}}

@Composable fun OrdersScreen(orders:List<Order>,onBack:()->Unit){Scaffold(topBar={TopAppBar(title={Text("طلباتي")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,"رجوع")}})}){p->if(orders.isEmpty())Box(Modifier.fillMaxSize().background(Background).padding(p),contentAlignment=Alignment.Center){Text("لا توجد طلبات بعد",color=TextGray)}else LazyColumn(Modifier.fillMaxSize().background(Background).padding(p).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(orders){o->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(o.service,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(o.address,color=TextGray);Text("الحالة: ${statusAr(o.status)}",color=Primary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp));Text(o.createdAt,fontSize=12.sp,color=TextGray,modifier=Modifier.padding(top=5.dp))}}}}}}

@Composable fun SimplePage(icon:@Composable()->Unit,title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().background(Background).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(45.dp));icon();Spacer(Modifier.height(15.dp));Text(title,fontSize=28.sp,fontWeight=FontWeight.Bold,color=TextDark);Text(subtitle,color=TextGray);Spacer(Modifier.height(25.dp));Column(Modifier.fillMaxWidth(),content=content)}}
@Composable fun PrimaryButton(text:String,loading:Boolean,onClick:()->Unit,icon:androidx.compose.ui.graphics.vector.ImageVector){Button(onClick,enabled=!loading,modifier=Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(15.dp)){if(loading)CircularProgressIndicator(Modifier.size(22.dp),color=Color.White,strokeWidth=2.dp)else{Icon(icon,null);Spacer(Modifier.width(8.dp));Text(text)}}}
@Composable fun ErrorText(error:String){if(error.isNotBlank()){Spacer(Modifier.height(8.dp));Text(error,color=Red,fontSize=13.sp)}}
fun statusAr(s:String)=when(s.uppercase()){"PENDING"->"قيد المراجعة";"CONFIRMED"->"مؤكد";"IN_PROGRESS"->"جاري التنفيذ";"COMPLETED"->"مكتمل";"CANCELLED"->"ملغي";else->s}

fun normalizeArabicDigits(v:String)=v.map{when(it){'٠'->'0';'١'->'1';'٢'->'2';'٣'->'3';'٤'->'4';'٥'->'5';'٦'->'6';'٧'->'7';'٨'->'8';'٩'->'9';else->it}}.joinToString("")
fun formatIraqiPhone(input:String):String?{val c=normalizeArabicDigits(input).trim().replace(" ","").replace("-","");return when{c.matches(Regex("^07[0-9]{9}$"))->"+964"+c.substring(1);c.matches(Regex("^\\+9647[0-9]{9}$"))->c;c.matches(Regex("^9647[0-9]{9}$"))->"+"+c;else->null}}

data class HttpResult(val code:Int,val body:String)
private suspend fun sendOtp(phone:String)=withContext(Dispatchers.IO){postAuth("/auth/v1/otp",JSONObject().put("phone",phone).toString()).let{if(it.code in 200..299)ApiResult(true,"تم إرسال رمز التحقق")else ApiResult(false,parseError(it.body,"تعذر إرسال الرمز"))}}
private suspend fun verifyOtp(phone:String,otp:String)=withContext(Dispatchers.IO){postAuth("/auth/v1/verify",JSONObject().put("phone",phone).put("token",otp).put("type","sms").toString()).let{if(it.code in 200..299){val j=JSONObject(it.body);ApiResult(true,"تم التحقق",j.optString("access_token"),j.optJSONObject("user")?.optString("id"))}else ApiResult(false,parseError(it.body,"رمز التحقق غير صحيح"))}}
private fun postAuth(path:String,body:String):HttpResult{val c=URL(SUPABASE_URL+path).openConnection() as HttpURLConnection;c.requestMethod="POST";c.connectTimeout=20000;c.readTimeout=20000;c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Authorization","Bearer $SUPABASE_KEY");c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val text=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()} ?: "";c.disconnect();return HttpResult(code,text)}
private fun parseError(body:String,fallback:String)=try{val j=JSONObject(body);j.optString("msg",j.optString("message",j.optString("error_description",fallback)))}catch(_:Exception){fallback}
private suspend fun saveProfile(token:String,userId:String,name:String,phone:String)=withContext(Dispatchers.IO){val body=JSONObject().put("id",userId).put("full_name",name).put("phone",phone).put("role","PATIENT").toString();rest("POST","/rest/v1/profiles",body,token,"resolution=merge-duplicates").let{if(it.code in 200..299)ApiResult(true,"تم حفظ الملف")else ApiResult(false,parseError(it.body,"تعذر حفظ الملف"))}}
private suspend fun fetchServices(token:String)=withContext(Dispatchers.IO){val r=rest("GET","/rest/v1/services?select=id,name_ar,description_ar,price&is_active=eq.true&order=name_ar",null,token);if(r.code !in 200..299)return@withContext emptyList<Service>();val a=JSONArray(r.body);buildList{for(i in 0 until a.length()){val j=a.getJSONObject(i);add(Service(j.getString("id"),j.optString("name_ar"),j.optString("description_ar"),j.optInt("price")))}}}
private suspend fun createBooking(token:String,userId:String,s:Service,address:String,notes:String,loc:LocationData)=withContext(Dispatchers.IO){val body=JSONObject().put("patient_id",userId).put("service_id",s.id).put("address",address).put("latitude",loc.lat).put("longitude",loc.lon).put("notes",notes).put("status","PENDING").toString();rest("POST","/rest/v1/bookings",body,token,"return=minimal").let{if(it.code in 200..299)ApiResult(true,"تم إنشاء الطلب")else ApiResult(false,parseError(it.body,"تعذر إرسال الطلب"))}}
private suspend fun fetchOrders(token:String,userId:String)=withContext(Dispatchers.IO){val r=rest("GET","/rest/v1/bookings?select=id,address,notes,status,latitude,longitude,created_at,services(name_ar)&patient_id=eq.$userId&order=created_at.desc",null,token);if(r.code !in 200..299)return@withContext emptyList<Order>();val a=JSONArray(r.body);buildList{for(i in 0 until a.length()){val j=a.getJSONObject(i);val s=j.optJSONObject("services")?.optString("name_ar") ?: "خدمة تمريض";add(Order(j.getString("id"),s,j.optString("address"),j.optString("notes"),if(j.isNull("latitude"))null else j.optDouble("latitude"),if(j.isNull("longitude"))null else j.optDouble("longitude"),j.optString("status"),formatIso(j.optString("created_at"))))}}}
data class RestResult(val code:Int,val body:String)
private fun rest(method:String,path:String,body:String?=null,token:String="",prefer:String?=null):RestResult{val c=URL(SUPABASE_URL+path).openConnection() as HttpURLConnection;c.requestMethod=method;c.connectTimeout=20000;c.readTimeout=20000;c.doInput=true;c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Authorization","Bearer ${if(token.isNotBlank())token else SUPABASE_KEY}");if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toByteArray())}};if(prefer!=null)c.setRequestProperty("Prefer",prefer);val code=c.responseCode;val text=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()} ?: "";c.disconnect();return RestResult(code,text)}
private fun formatIso(v:String)=try{val f=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",Locale.US);SimpleDateFormat("yyyy/MM/dd - HH:mm",Locale.getDefault()).format(f.parse(v) ?: Date())}catch(_:Exception){v.take(16).replace('T',' ')}
