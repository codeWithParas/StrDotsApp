package com.xyz.strapp.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyz.strapp.R
import kotlinx.coroutines.delay

@Preview
@Composable
fun StartScreenPreview() {
    StartScreen({})
}

@Composable
fun StartScreen(
    nextScreen: () -> Unit,
) {
    // --- ADD ANIMATION STATE ---
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500) // Animation duration
    )

    //start a timer for 10 seconds then navigate to the next screen
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        nextScreen()
    }


    Box(
        modifier = Modifier.fillMaxSize().alpha(alphaAnimation.value),
        contentAlignment = Alignment.Center
    ){
        BackgroundImage(modifier = Modifier)
//        Box(
//            modifier = Modifier
//                .size(150.dp)
//                .clip(CircleShape)
//                .background(Color.Blue, CircleShape)
//                .clickable {
//                    nextScreen()
//                },
//            contentAlignment = Alignment.Center
//        ){
//            Text(
//                text = "Start",
//                fontSize = 35.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//        }
    }

}

@Composable
fun BackgroundImage(modifier: Modifier){
    val image = painterResource(R.drawable.splash)
    Image(
        modifier = Modifier.fillMaxSize(),

        painter = image,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}