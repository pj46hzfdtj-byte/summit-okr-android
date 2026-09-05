package com.visokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visokr.android.core.AuthVM
import com.visokr.android.ui.AuroraOverlay
import com.visokr.android.ui.BrandGradient
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.isDarkTheme

@Composable
fun LoginPage(onLoggedIn: () -> Unit) {
    val vm: AuthVM = viewModel()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var email by remember { mutableStateOf("demo@visokr.com") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("password123") }
    var obscure by remember { mutableStateOf(true) }
    var register by remember { mutableStateOf(false) }
    val t = LocalVisTokens.current

    // 登录成功自动跳转
    val state by vm.state.collectAsState()
    if (state is com.visokr.android.core.UiState.Success && com.visokr.android.core.TokenStore.accessToken != null) {
        val user = (state as com.visokr.android.core.UiState.Success<*>).data
        if (user != null) {
            androidx.compose.runtime.LaunchedEffect(user) { onLoggedIn() }
        }
    }

    Box(Modifier.fillMaxSize().background(if (t.macos) Color.Transparent else MaterialTheme.colorScheme.background)) {
        AuroraOverlay()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 420.dp)) {
                Spacer(Modifier.height(24.dp))
                // 品牌徽标（居中 + 主色光晕，对齐 Flutter _BrandBadge）
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(76.dp)
                            .shadow(
                                10.dp, RoundedCornerShape(22.dp), clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary,
                            )
                            .clip(RoundedCornerShape(22.dp)).background(BrandGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "VIS OKR",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "让每一个目标，都被认真达成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(36.dp))
                // 玻璃表单卡片
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(if (isDarkTheme()) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.88f))
                        .padding(24.dp),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱") },
                        leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (register) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名") },
                            leadingIcon = { Icon(Icons.Outlined.PersonOutline, null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { obscure = !obscure }) {
                                Icon(if (obscure) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, modifier = Modifier.size(20.dp))
                            }
                        },
                        visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    // 渐变主按钮
                    Button(
                        onClick = {
                            if (register) vm.register(email.trim(), username.trim(), password)
                            else vm.login(email.trim(), password)
                        },
                        enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!register || username.isNotBlank()),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(
                                8.dp, RoundedCornerShape(12.dp), clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(BrandGradient),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            else Text(if (register) "注册" else "登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (register) "已有账号？" else "还没有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { register = !register }) {
                        Text(if (register) "登录" else "注册")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "演示账号 demo@visokr.com / password123",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}