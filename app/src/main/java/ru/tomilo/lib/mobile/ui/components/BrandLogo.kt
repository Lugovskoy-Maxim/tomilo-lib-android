package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@Composable
fun TomiloRingLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Image(
        painter = painterResource(R.drawable.logo_tomilo_ring),
        contentDescription = "Tomilo",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun TomiloWordmark(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 220.dp,
) {
    Image(
        painter = painterResource(R.drawable.logo_tomilo_wordmark),
        contentDescription = "Tomilo Lib",
        modifier = modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth(),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
fun TomiloBrandHeader(
    subtitle: String? = "Библиотека манги и манхвы",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                ) {
                    append("TOMILO")
                }
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                ) {
                    append(" LIB")
                }
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
            ),
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TomiloMuted,
            )
        }
    }
}

@Composable
fun TomiloMascot(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
) {
    Image(
        painter = painterResource(R.drawable.logo_tomilo_mascot),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun TomiloLoginIllustration(
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    Image(
        painter = painterResource(R.drawable.illust_login_character),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentScale = ContentScale.Fit,
    )
}
