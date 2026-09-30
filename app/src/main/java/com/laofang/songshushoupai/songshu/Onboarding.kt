// 松鼠兽牌 一款旧手机变兽牌的软件
// Copyright (C) 2026  laofang
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.laofang.songshushoupai.songshu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.laofang.songshushoupai.songshu.settings.TutorialSettingsCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private enum class OnbStep { Welcome, Tutorial, SubOptions, Ready }

/** 步骤在向导中的线性位置，用于判断滑动方向（子选项详情比列表更深一层） */
private fun onbPosition(step: OnbStep, sub: String?): Int = when (step) {
    OnbStep.Welcome -> 0
    OnbStep.Tutorial -> 1
    OnbStep.SubOptions -> if (sub != null) 3 else 2
    OnbStep.Ready -> 4
}

/**
 * 首次启动的新手引导向导。
 * 流程：欢迎（中英文轮换）→ 使用教程 → 设置（基本/二维码/主题/备份，返回回到列表）→ 一切准备就绪。
 * 最后一步点击圆形「>」按钮：箭头消失、按钮放大铺满并淡入主页（通过 [onFinish] 交由宿主切换）。
 */
@Composable
fun OnboardingFlow(
    onThemeChanged: (Int) -> Unit,
    onDarkModeChanged: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stepOrder = listOf(OnbStep.Welcome, OnbStep.Tutorial, OnbStep.SubOptions, OnbStep.Ready)
    var stepIdx by rememberSaveable { mutableIntStateOf(0) }
    val step = stepOrder[stepIdx]
    // 子选项列表下钻到具体设置页；null 表示停留在列表
    var subDetail by rememberSaveable { mutableStateOf<String?>(null) }

    // 结束动画：箭头淡出 + 从按钮中心放大铺满 + 整体淡出露出主页
    var finishing by remember { mutableStateOf(false) }
    var launchCenter by remember { mutableStateOf(Offset.Zero) }
    val arrowAlpha = remember { Animatable(1f) }
    val circleScale = remember { Animatable(1f) }
    val rootAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun finishAnimated() {
        if (finishing) return
        finishing = true
        scope.launch {
            launch { arrowAlpha.animateTo(0f, tween(160)) }
            circleScale.animateTo(70f, tween(620, easing = EaseInOut))
            rootAlpha.animateTo(0f, tween(360, easing = EaseInOut))
            onFinish()
        }
    }

    // 动画播放期间屏蔽返回；子选项详情返回回到列表；其余回到上一步
    BackHandler(enabled = finishing) {}
    BackHandler(enabled = subDetail != null) { subDetail = null }
    BackHandler(enabled = subDetail == null && stepIdx > 0 && !finishing) { stepIdx -= 1 }

    val showBottomBar = !finishing && step in listOf(OnbStep.Tutorial, OnbStep.SubOptions) && subDetail == null
    val title = when {
        subDetail != null -> when (subDetail) {
            "basic" -> stringResource(R.string.basic_settings)
            "qrcode" -> stringResource(R.string.qrcode_settings)
            "theme" -> stringResource(R.string.theme_settings)
            else -> stringResource(R.string.backup_settings)
        }
        step == OnbStep.SubOptions -> stringResource(R.string.onboarding_suboptions_title)
        step == OnbStep.Tutorial -> stringResource(R.string.tutorial_settings)
        else -> stringResource(R.string.app_name)
    }

    // 引导根节点没有 Surface 提供 contentColor，复用的设置页中标题多用默认色（依赖 LocalContentColor），
    // 暗色模式下会回退成深色导致“标题显示异常”，这里显式提供与背景适配的内容色
    CompositionLocalProvider(LocalContentColor provides cs.onBackground) {
    // pointerInput + 空 detectTapGestures 吃掉落在空白处的点击，避免事件穿透到下层主页触发其可点击项
    Box(
        Modifier.fillMaxSize().alpha(rootAlpha.value).background(cs.background)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(Modifier.fillMaxSize()) {
            // 顶栏：步骤 1/5 隐藏（标题整体从上方往下滑入/滑出）；2/3/4 居中显示标题
            // 抽成独立 @Composable，避免在 Column 作隐式接收者时 AnimatedVisibility 误解析到 ColumnScope 重载
            val showTopBar = step != OnbStep.Welcome && step != OnbStep.Ready
            OnbTopBar(
                visible = showTopBar,
                title = title,
            )

            // 内容区：步骤间水平滑动过渡，方向随前进/后退而相反
            AnimatedContent(
                targetState = step to subDetail,
                transitionSpec = {
                    val forward = onbPosition(targetState.first, targetState.second) >=
                            onbPosition(initialState.first, initialState.second)
                    if (forward) {
                        slideInHorizontally(tween(300)) { it } togetherWith slideOutHorizontally(tween(300)) { -it }
                    } else {
                        slideInHorizontally(tween(300)) { -it } togetherWith slideOutHorizontally(tween(300)) { it }
                    }
                },
                label = "onboardingContent",
                modifier = Modifier.weight(1f)
            ) { (s, sub) ->
                when (s) {
                    OnbStep.Welcome -> WelcomeStep(onGetStarted = { stepIdx = 1 })
                    OnbStep.Tutorial -> Column(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                        ) {
                            Spacer(Modifier.height(16.dp))
                            TutorialSettingsCard()
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    OnbStep.SubOptions -> if (sub != null) {
                        when (sub) {
                            "basic" -> BasicSettingsPage()
                            "qrcode" -> QrCodeSettingsPage()
                            "theme" -> ThemeSettingsPage(onThemeChanged, onDarkModeChanged)
                            else -> BackupSettingsPage()
                        }
                    } else {
                        SubOptionsList(onOpen = { subDetail = it })
                    }
                    OnbStep.Ready -> ReadyStep(
                        onLaunch = { finishAnimated() },
                        onCentered = { launchCenter = it },
                        arrowVisible = !finishing
                    )
                }
            }

            // 底部导航：上一步 / 下一步（按钮整体从下方往上滑入/滑出；外层固定等高容器，内容区不跳动）
            // 同样抽成独立 @Composable，消除 ColumnScope 隐式接收者导致的 AnimatedVisibility 解析歧义
            val showReturnBar = step == OnbStep.SubOptions && subDetail != null && !finishing
            OnbBottomBar(
                showNav = showBottomBar,
                showReturn = showReturnBar,
                onPrev = { if (stepIdx > 0) stepIdx -= 1 },
                onNext = { if (stepIdx < stepOrder.lastIndex) stepIdx += 1 },
                onReturn = { subDetail = null },
            )
        }

        // 结束时的放大圆层：从按钮实际中心铺满全屏，随后随根层整体淡出露出主页
        if (finishing) {
            val rPx = with(LocalDensity.current) { 72.dp.toPx() / 2f }
            Box(
                modifier = Modifier.fillMaxSize().zIndex(10f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            translationX = launchCenter.x - rPx
                            translationY = launchCenter.y - rPx
                            scaleX = circleScale.value
                            scaleY = circleScale.value
                        }
                        .background(cs.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(32.dp).alpha(arrowAlpha.value)
                    )
                }
            }
        }
    }
    }
}

/** 顶栏：固定 72dp 等高容器，标题从上方竖向滑入/滑出，保证内容区高度恒定 */
@Composable
private fun OnbTopBar(
    visible: Boolean,
    title: String,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 4.dp).height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** 底部导航栏：固定 78dp 等高容器，上一步/下一步与子页返回按钮从下方竖向滑入/滑出，保证内容区高度恒定 */
@Composable
private fun OnbBottomBar(
    showNav: Boolean,
    showReturn: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReturn: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(78.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        AnimatedVisibility(
            visible = showNav,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.onboarding_back)) }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.onboarding_next)) }
            }
        }
        // 子页面（二维码/主题/备份）底部返回：回到子选项列表；与上下步按钮等高，保持内容区恒定避免跳动
        AnimatedVisibility(
            visible = showReturn,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.onboarding_return))
            }
        }
    }
}

/** 欢迎页：上半屏居中展示未裁切的松鼠 logo，下半屏居中放中英文「欢迎 / Welcome」轮换、副标题与开始按钮 */
@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val words = listOf(
        stringResource(R.string.onboarding_welcome_zh),
        stringResource(R.string.onboarding_welcome_en)
    )
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500.milliseconds)
            idx = (idx + 1) % words.size
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 上半屏：松鼠 logo 居中展示，保持原始方形不裁切
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.qidong),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
        }
        // 下半屏：欢迎词、副标题与开始按钮整组居中
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = idx,
                    transitionSpec = {
                        // 旧字上滑淡出并微缩，新字从下方滚入、淡入并轻微放大到位，缓动统一为 EaseInOut，过渡更连贯
                        val enter = fadeIn(tween(360, easing = EaseInOut)) +
                                slideInVertically(tween(420, easing = EaseOut)) { it / 2 } +
                                scaleIn(tween(420, easing = EaseOut), initialScale = 0.86f)
                        val exit = fadeOut(tween(240, easing = EaseIn)) +
                                slideOutVertically(tween(320, easing = EaseIn)) { -it / 2 } +
                                scaleOut(tween(320, easing = EaseIn), targetScale = 0.9f)
                        enter togetherWith exit
                    },
                    label = "welcomeWord"
                ) { i ->
                    Text(
                        text = words[i],
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                modifier = Modifier.height(52.dp).padding(horizontal = 32.dp)
            ) {
                Text(stringResource(R.string.onboarding_get_started), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** 子选项列表：与设置页统一的 NavRow 卡片样式，整组垂直居中 */
@Composable
private fun SubOptionsList(onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.CenterVertically)
    ) {
        NavRow(Icons.Outlined.Tune, stringResource(R.string.basic_settings)) { onOpen("basic") }
        NavRow(Icons.Outlined.QrCode2, stringResource(R.string.qrcode_settings)) { onOpen("qrcode") }
        NavRow(Icons.Outlined.ColorLens, stringResource(R.string.theme_settings)) { onOpen("theme") }
        NavRow(Icons.Outlined.SettingsBackupRestore, stringResource(R.string.backup_settings)) { onOpen("backup") }
    }
}

/** 一切准备就绪：图标 + 文案整组居中，圆形「>」按钮固定在底部 */
@Composable
private fun ReadyStep(
    onLaunch: () -> Unit,
    onCentered: (Offset) -> Unit,
    arrowVisible: Boolean,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标 + 标题 + 描述整组在剩余空间里垂直居中
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.lihua),
                contentDescription = null,
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onboarding_ready_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_ready_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        // 圆形「>」按钮固定在底部（预留导航栏内边距避免遮挡）
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .size(72.dp)
                .onGloballyPositioned { coords -> onCentered(coords.boundsInRoot().center) }
                .clip(CircleShape)
                .clickable(onClick = onLaunch),
            shape = CircleShape,
            color = cs.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.onboarding_get_started),
                    modifier = Modifier.size(32.dp).alpha(if (arrowVisible) 1f else 0f),
                    tint = cs.onPrimary
                )
            }
        }
    }
}
