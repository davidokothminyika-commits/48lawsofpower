package com.example.a48lawsofpower.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import com.example.a48lawsofpower.ui.theme.Inter
import com.example.a48lawsofpower.ui.theme.LawTheme
import com.example.a48lawsofpower.ui.theme.PaperText
import com.example.a48lawsofpower.ui.theme.PaperTextMuted
import com.example.a48lawsofpower.ui.theme.Playfair
import com.example.a48lawsofpower.ui.theme.Ink
import com.example.a48lawsofpower.ui.theme.InkText
import com.example.a48lawsofpower.ui.theme.InkTextMuted
import com.example.a48lawsofpower.ui.theme.reducedMotion
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
 * The title is set in the bundled Playfair Display; see `ATTRIBUTIONS.md` for the licence and the
 * subsetting note.
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entrance = remember { Animatable(0f) }

    /**
     * The ambient breath: a slow swell on the piece so the entry screen is alive rather than a still
     * frame waiting on a tap.
     *
     * Deliberately shallow - 1.5% of scale over 1.9s, easing in and out - because this loop runs
     * indefinitely and anything more insistent becomes an annoyance rather than a signpost. The
     * alpha rides the same phase, a little lighter at the bottom of the swell, which is what stops
     * it reading as a mechanical zoom.
     *
     * Under reduced motion it collapses to a constant 1.0 and does not animate at all. A perpetual
     * loop is the one animation that cannot be shortened usefully, so the honest response is to drop
     * it rather than run it faster.
     */
    val calm = reducedMotion()
    val breathe by rememberInfiniteTransition(label = "splash-breath").animateFloat(
        initialValue = 1f,
        targetValue = if (calm) 1f else 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (calm) 0 else 1900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = if (calm) RepeatMode.Restart else RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    // The splash paints its own surface rather than reading `background`, because it is the first
    // thing on screen and a hard-wired near-black slab flashes badly for a reader who chose light.
    // A faint vertical wash either way so the piece sits in a space rather than on a flat field.
    val canvasTop: Color
    val canvasBottom: Color
    val titleInk: Color
    val subInk: Color
    if (LawTheme.isDark) {
        canvasTop = Color(0xFF16161A)
        canvasBottom = Color(0xFF0B0B0D)
        titleInk = InkText
        subInk = InkTextMuted
    } else {
        canvasTop = Color(0xFFFDFEFF)
        canvasBottom = Color(0xFFEDF1F6)
        titleInk = PaperText
        subInk = PaperTextMuted
    }
    // Guidelines section 3: a launch logo scales from ~96% to 100% over 180-220ms, and there is no
    // long animated intro. The piece also fades, and under reduced motion both collapse to zero.
    val duration = motionDuration(Motion.STANDARD)
    LaunchedEffect(duration) {
        entrance.animateTo(1f, tween(durationMillis = duration, easing = Motion.Settle))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(canvasTop, canvasBottom))),
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
                    // Two animations on one piece, and they multiply rather than fight: the entrance
                    // is a one-shot settle, the breath is the ambient loop underneath it.
                    .alpha(entrance.value * (0.86f + 0.14f * breathe))
                    .scale((0.96f + 0.04f * entrance.value) * breathe),
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "48 Laws",
                color = CrimsonBright,
                fontFamily = Playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(entrance.value),
            )
            Text(
                text = "of Power",
                color = titleInk,
                fontFamily = Playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(entrance.value),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Read. Understand. Apply.",
                color = subInk,
                fontFamily = Inter,
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