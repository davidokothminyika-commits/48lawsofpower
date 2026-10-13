package com.example.a48lawsofpower.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a48lawsofpower.R
import com.example.a48lawsofpower.ui.theme.Crimson
import com.example.a48lawsofpower.ui.theme.CrimsonBright
import com.example.a48lawsofpower.ui.theme.Ink
import com.example.a48lawsofpower.ui.theme.InkText
import com.example.a48lawsofpower.ui.theme.InkTextMuted
import com.example.a48lawsofpower.ui.theme.Motion
import com.example.a48lawsofpower.ui.theme.motionDuration

/**
 * The entry screen: title, motto, and one button into the Dossier.
 *
 * There is no Sign Up or Log In here, deliberately. The design showed both, plus Continue with
 * Google, but every one of those needs a network call and a Google dependency, and the app's
 * Privacy Policy tells the reader nothing ever leaves the device. Shipping those screens would
 * have made the app's own published promise false, so the account surface is absent rather than
 * faked. See NOTICE.md.
 *
 * [FontFamily.Serif] stands in for Playfair Display until the real face is bundled; Android maps
 * it to Noto Serif, which has a similar high-contrast bookish skeleton. Bundling Playfair is
 * tracked in ATTRIBUTIONS.md.
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entrance = remember { Animatable(0f) }
    // Guidelines section 3: a launch logo scales from ~96% to 100% over 180-220ms, and there is no
    // long animated intro. The piece also fades, and under reduced motion both collapse to zero.
    val duration = motionDuration(Motion.STANDARD)
    LaunchedEffect(duration) {
        entrance.animateTo(1f, tween(durationMillis = duration, easing = Motion.Settle))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // A faint vertical wash so the piece sits in a space rather than on a flat slab.
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF16161A), Ink, Color(0xFF0B0B0D)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.splash_chess_king),
                contentDescription = null,
                modifier = Modifier
                    .size(148.dp)
                    .alpha(entrance.value)
                    .scale(0.96f + 0.04f * entrance.value),
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "48 Laws",
                color = CrimsonBright,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(entrance.value),
            )
            Text(
                text = "of Power",
                color = InkText,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(entrance.value),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Read. Understand. Apply.",
                color = InkTextMuted,
                fontSize = 15.sp,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(entrance.value),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Crimson,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview
@Composable
private fun SplashPreview() {
    SplashScreen(onGetStarted = {})
}