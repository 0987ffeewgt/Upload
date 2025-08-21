package com.example.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SnakeGame()
                }
            }
        }
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class Point(val x: Int, val y: Int)

@Composable
fun SnakeGame() {
    var isRunning by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val gridSize = 20 // 20x20
    val cellPadding = 1.dp

    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var snake by remember {
        mutableStateOf(listOf(Point(5, 10), Point(4, 10), Point(3, 10)))
    }
    var food by remember { mutableStateOf(randomFood(gridSize, snake)) }
    var gameOver by remember { mutableStateOf(false) }

    // Game loop
    LaunchedEffect(isRunning, direction, snake, gameOver) {
        while (isRunning && !gameOver) {
            delay(130L) // speed
            val newHead = nextHead(snake.first(), direction, gridSize)
            // Check self-collision
            if (snake.contains(newHead)) {
                gameOver = true
                isRunning = false
                continue
            }
            var newSnake = listOf(newHead) + snake.dropLast(1)
            if (newHead == food) {
                // grow
                newSnake = listOf(newHead) + snake
                score += 1
                food = randomFood(gridSize, newSnake)
            }
            snake = newSnake
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(12.dp)
    ) {
        Text(
            text = if (gameOver) "Игра окончена — счёт: $score" else "Счёт: $score",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .aspectRatio(1f)
                .background(Color(0xFF151515))
                .pointerInput(Unit) {
                    // swipe detection
                    detectTransformGesturesOrSwipes(onSwipe = { dx, dy ->
                        direction = when {
                            abs(dx) > abs(dy) && dx > 0 && direction != Direction.LEFT -> Direction.RIGHT
                            abs(dx) > abs(dy) && dx < 0 && direction != Direction.RIGHT -> Direction.LEFT
                            abs(dy) > abs(dx) && dy > 0 && direction != Direction.UP -> Direction.DOWN
                            abs(dy) > abs(dx) && dy < 0 && direction != Direction.DOWN -> Direction.UP
                            else -> direction
                        }
                    })
                }
        ) {
            val cellSize: Dp = maxWidth / gridSize
            Canvas(Modifier.fillMaxSize()) {
                // draw grid (light)
                val step = size.width / gridSize
                for (i in 0..gridSize) {
                    // Vertical lines
                    drawLine(
                        color = Color(0xFF202020),
                        start = Offset(i * step, 0f),
                        end = Offset(i * step, size.height)
                    )
                    // Horizontal lines
                    drawLine(
                        color = Color(0xFF202020),
                        start = Offset(0f, i * step),
                        end = Offset(size.width, i * step)
                    )
                }

                // draw food
                drawRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(food.x * step + cellPadding.toPx(), food.y * step + cellPadding.toPx()),
                    size = androidx.compose.ui.geometry.Size(step - 2*cellPadding.toPx(), step - 2*cellPadding.toPx())
                )
                // draw snake
                snake.forEachIndexed { index, p ->
                    drawRect(
                        color = if (index == 0) Color(0xFF66BB6A) else Color(0xFF43A047),
                        topLeft = Offset(p.x * step + cellPadding.toPx(), p.y * step + cellPadding.toPx()),
                        size = androidx.compose.ui.geometry.Size(step - 2*cellPadding.toPx(), step - 2*cellPadding.toPx())
                    )
                }
            }
        }

        if (gameOver) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(onClick = {
                    score = 0
                    snake = listOf(Point(5, 10), Point(4, 10), Point(3, 10))
                    direction = Direction.RIGHT
                    food = randomFood(gridSize, snake)
                    gameOver = false
                }) {
                    Text("Заново")
                }
                Button(onClick = { isRunning = true }) { Text("Старт") }
            }
        } else {
            // Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { if (direction != Direction.DOWN) direction = Direction.UP }) { Text("▲") }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { if (direction != Direction.RIGHT) direction = Direction.LEFT }) { Text("◀") }
                    if (isRunning) {
                        Button(onClick = { isRunning = false }) { Text("Пауза") }
                    } else {
                        Button(onClick = { isRunning = true }) { Text("Старт") }
                    }
                    OutlinedButton(onClick = { if (direction != Direction.LEFT) direction = Direction.RIGHT }) { Text("▶") }
                }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { if (direction != Direction.UP) direction = Direction.DOWN }) { Text("▼") }
                }
            }
        }
    }
}

private fun nextHead(head: Point, dir: Direction, grid: Int): Point {
    val x = when (dir) {
        Direction.LEFT -> (head.x - 1 + grid) % grid
        Direction.RIGHT -> (head.x + 1) % grid
        else -> head.x
    }
    val y = when (dir) {
        Direction.UP -> (head.y - 1 + grid) % grid
        Direction.DOWN -> (head.y + 1) % grid
        else -> head.y
    }
    return Point(x, y)
}

private fun randomFood(grid: Int, snake: List<Point>): Point {
    var p: Point
    do {
        p = Point(Random.nextInt(grid), Random.nextInt(grid))
    } while (snake.contains(p))
    return p
}

// Simple swipe detector using pointer input; minimal approach without dependencies
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.changedToUp

suspend fun PointerInputScope.detectTransformGesturesOrSwipes(onSwipe: (dx: Float, dy: Float) -> Unit) {
    while (true) {
        val down = awaitPointerEventScope { awaitFirstDown() }
        var startX = down.position.x
        var startY = down.position.y
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val anyUp = event.changes.any(PointerInputChange::changedToUp)
                val current = event.changes.firstOrNull()
                if (current != null) {
                    val dx = current.position.x - startX
                    val dy = current.position.y - startY
                    if (abs(dx) > 40f || abs(dy) > 40f) {
                        onSwipe(dx, dy)
                        startX = current.position.x
                        startY = current.position.y
                    }
                }
                if (anyUp) break
            }
        }
    }
}