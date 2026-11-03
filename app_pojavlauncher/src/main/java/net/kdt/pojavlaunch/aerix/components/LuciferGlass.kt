package net.kdt.pojavlaunch.aerix.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private val DotBlue = Color(0xFF4D8DFF)
private val DotRed = Color(0xFFFF4D5E)

/** Frosted white glass: soft gradient fill, bright top edge and a thin light border. */
fun Modifier.luciferGlass(shape: Shape, strong: Boolean = false): Modifier {
    val top = if (strong) 0.42f else 0.22f
    val mid = if (strong) 0.16f else 0.07f
    val low = if (strong) 0.22f else 0.12f
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = top), Color.White.copy(alpha = mid), Color.White.copy(alpha = low))
            ),
            shape
        )
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                size = Size(size.width, size.height * 0.46f)
            )
        }
        .border(1.dp, Color.White.copy(alpha = if (strong) 0.7f else 0.34f), shape)
}

/** Tiny sparse blue and red dots on the black canvas. */
@Composable
fun LuciferDots(modifier: Modifier = Modifier) {
    val dots = remember {
        val rnd = Random(7)
        List(70) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextInt(2) == 0) }
    }
    Canvas(modifier.fillMaxSize()) {
        dots.forEach { (fx, fy, blue) ->
            val c = if (blue) DotBlue else DotRed
            val at = Offset(fx * size.width, fy * size.height)
            drawCircle(c.copy(alpha = 0.16f), radius = 4.dp.toPx(), center = at)
            drawCircle(c.copy(alpha = 0.9f), radius = 1.1.dp.toPx(), center = at)
        }
    }
}

@Composable
fun LuciferGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    height: Dp = 44.dp,
    strong: Boolean = false,
    cornerRadius: Dp = 14.dp,
    fontSize: TextUnit = 14.sp,
    letterSpacing: TextUnit = 1.6.sp,
    center: Boolean = true
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    Row(
        modifier = modifier
            .height(height)
            .luciferGlass(shape, strong)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (center) Arrangement.Center else Arrangement.spacedBy(12.dp)
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(fontSize.value.dp * 1.4f)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(
            text = text.uppercase(),
            color = Color.White,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
