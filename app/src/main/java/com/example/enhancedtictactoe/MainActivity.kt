package com.example.enhancedtictactoe

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

data class ArenaTheme(
    val name: String,
    val bgDark: Int,
    val cardBg: Int,
    val tileBg: Int,
    val tileBorder: Int,
    val playerXColor: Int,
    val playerOColor: Int,
    val accentColor: Int,
    val winColor: Int
)

class MainActivity : Activity() {
    private var player1Name = "Player 1"
    private var player2Name = "Player 2"

    // 4x4 Board (16 cells: indices 0..15)
    private val board = Array(16) { "" }
    private val cells = ArrayList<Button>()
    private val moveLog = ArrayList<String>()

    private var startingPlayer = "X"
    private var currentPlayer = "X"
    private var moveCount = 0
    private var gameOver = false
    private var gameMode = "TWO_PLAYER" // "TWO_PLAYER" or "SINGLE_PLAYER"

    private var scoreX = 0
    private var scoreO = 0
    private var draws = 0
    private var streakPlayer = ""
    private var streakCount = 0

    private lateinit var statusBanner: TextView
    private lateinit var moveCounterText: TextView
    private lateinit var p1Card: LinearLayout
    private lateinit var p2Card: LinearLayout
    private lateinit var p1ScoreLabel: TextView
    private lateinit var p2ScoreLabel: TextView
    private lateinit var drawScoreLabel: TextView

    private val handler = Handler(Looper.getMainLooper())

    // All 10 winning lines of 4 identical symbols on a 4x4 grid
    private val winPatterns4x4 = arrayOf(
        // 4 Horizontal rows
        intArrayOf(0, 1, 2, 3),
        intArrayOf(4, 5, 6, 7),
        intArrayOf(8, 9, 10, 11),
        intArrayOf(12, 13, 14, 15),
        // 4 Vertical columns
        intArrayOf(0, 4, 8, 12),
        intArrayOf(1, 5, 9, 13),
        intArrayOf(2, 6, 10, 14),
        intArrayOf(3, 7, 11, 15),
        // 2 Diagonals
        intArrayOf(0, 5, 10, 15),
        intArrayOf(3, 6, 9, 12)
    )

    private val themes = listOf(
        ArenaTheme(
            name = "Cosmic Neon",
            bgDark = Color.parseColor("#0B0E1E"),
            cardBg = Color.parseColor("#151A33"),
            tileBg = Color.parseColor("#1E2547"),
            tileBorder = Color.parseColor("#323D70"),
            playerXColor = Color.parseColor("#00E5FF"),
            playerOColor = Color.parseColor("#FF3D81"),
            accentColor = Color.parseColor("#7C4DFF"),
            winColor = Color.parseColor("#00E676")
        ),
        ArenaTheme(
            name = "Cyber Sunset",
            bgDark = Color.parseColor("#160D1C"),
            cardBg = Color.parseColor("#261530"),
            tileBg = Color.parseColor("#351D42"),
            tileBorder = Color.parseColor("#59326E"),
            playerXColor = Color.parseColor("#FFAB00"),
            playerOColor = Color.parseColor("#FF4081"),
            accentColor = Color.parseColor("#FF6D00"),
            winColor = Color.parseColor("#69F0AE")
        ),
        ArenaTheme(
            name = "Royal Emerald",
            bgDark = Color.parseColor("#0D1918"),
            cardBg = Color.parseColor("#142927"),
            tileBg = Color.parseColor("#1D3B38"),
            tileBorder = Color.parseColor("#2E5C57"),
            playerXColor = Color.parseColor("#64FFDA"),
            playerOColor = Color.parseColor("#FFD740"),
            accentColor = Color.parseColor("#00BFA5"),
            winColor = Color.parseColor("#76FF03")
        )
    )
    private var themeIndex = 0
    private val activeTheme: ArenaTheme
        get() = themes[themeIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLobbyScreen()
    }

    // ==================== 1. CREATIVE LOBBY & PLAYER SETUP ====================
    private fun showLobbyScreen() {
        val t = activeTheme
        val scroll = ScrollView(this).apply {
            setBackgroundColor(t.bgDark)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 40, 36, 40)
        }

        // Top Bar: Live Theme Switcher Pill
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val themeBtn = Button(this).apply {
            text = "THEME: ${t.name.uppercase()}"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(24, 8, 24, 8)
            background = createRoundedBox(t.cardBg, t.accentColor, 2, 50f)
            setOnClickListener {
                themeIndex = (themeIndex + 1) % themes.size
                showLobbyScreen()
            }
        }
        topRow.addView(themeBtn)
        root.addView(topRow)

        // Hero Title
        val badge = TextView(this).apply {
            text = "4 × 4 TACTICAL EDITION"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerXColor)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 6)
        }

        val title = TextView(this).apply {
            text = "TIC · TAC · TOE"
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(18f, 0f, 0f, t.accentColor)
        }

        val subtitle = TextView(this).apply {
            text = "16 Squares · 10 Winning Lines · Connect 4 in a Row"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 28)
        }

        root.addView(badge)
        root.addView(title)
        root.addView(subtitle)

        // Player Names Card
        val setupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = createRoundedBox(t.cardBg, t.tileBorder, 2, 24f)
        }

        val setupHeader = TextView(this).apply {
            text = "PLAYER ROSTER"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 14)
        }

        val p1Label = TextView(this).apply {
            text = "Player 1 — Symbol X"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerXColor)
        }
        val p1Input = EditText(this).apply {
            setText(player1Name)
            hint = "Enter Player 1 Name"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            setPadding(24, 18, 24, 18)
            background = createRoundedBox(t.tileBg, t.playerXColor, 2, 16f)
        }

        val p2Label = TextView(this).apply {
            text = "Player 2 — Symbol O (Two-Player Mode)"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerOColor)
            setPadding(0, 16, 0, 0)
        }
        val p2Input = EditText(this).apply {
            setText(player2Name)
            hint = "Enter Player 2 Name"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            setPadding(24, 18, 24, 18)
            background = createRoundedBox(t.tileBg, t.playerOColor, 2, 16f)
        }

        val inputParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 6) }

        setupCard.addView(setupHeader)
        setupCard.addView(p1Label)
        setupCard.addView(p1Input, inputParams)
        setupCard.addView(p2Label)
        setupCard.addView(p2Input, inputParams)

        val setupParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 22) }
        root.addView(setupCard, setupParams)

        // Start Buttons
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 12, 0, 0) }

        val twoPlayerBtn = Button(this).apply {
            text = "START 4×4 TWO-PLAYER GAME"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            setPadding(20, 24, 20, 24)
            background = createRoundedBox(t.playerXColor, t.playerXColor, 0, 20f)
            setOnClickListener {
                player1Name = p1Input.text.toString().trim().ifEmpty { "Player 1" }
                player2Name = p2Input.text.toString().trim().ifEmpty { "Player 2" }
                gameMode = "TWO_PLAYER"
                resetSessionStats()
                createArenaScreen()
            }
        }

        val singlePlayerBtn = Button(this).apply {
            text = "BONUS: 4×4 VS TACTICAL AI"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(20, 20, 20, 20)
            background = createRoundedBox(t.accentColor, t.accentColor, 0, 20f)
            setOnClickListener {
                player1Name = p1Input.text.toString().trim().ifEmpty { "Player 1" }
                player2Name = "Tactical AI"
                gameMode = "SINGLE_PLAYER"
                resetSessionStats()
                createArenaScreen()
            }
        }

        root.addView(twoPlayerBtn, btnParams)
        root.addView(singlePlayerBtn, btnParams)

        // Rules Card
        val rulesCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            background = createRoundedBox(t.cardBg, t.tileBorder, 1, 20f)
        }
        val rulesTitle = TextView(this).apply {
            text = "4×4 VICTORY CONDITIONS"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerOColor)
        }
        val rulesBody = TextView(this).apply {
            text = "• Player 1 (X) and Player 2 (O) alternate turns on a 4×4 grid.\n" +
                "• Align 4 matching symbols horizontally, vertically, or diagonally to win.\n" +
                "• If all 16 tiles fill with no 4-in-a-row line, the round ends in a draw."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 0)
        }
        rulesCard.addView(rulesTitle)
        rulesCard.addView(rulesBody)

        val rulesParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 24, 0, 0) }
        root.addView(rulesCard, rulesParams)

        scroll.addView(root)
        setContentView(scroll)
    }

    // ==================== 2. 4×4 ARENA GAME SCREEN ====================
    private fun createArenaScreen() {
        val t = activeTheme
        cells.clear()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(t.bgDark)
            isFillViewport = true
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 28, 28, 36)
        }

        // Top Navigation Bar
        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val backBtn = Button(this).apply {
            text = "← LOBBY"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(20, 8, 20, 8)
            background = createRoundedBox(t.cardBg, t.tileBorder, 2, 40f)
            setOnClickListener {
                handler.removeCallbacksAndMessages(null)
                showLobbyScreen()
            }
        }

        val arenaTag = TextView(this).apply {
            text = "4×4 ARENA"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val themeCycleBtn = Button(this).apply {
            text = "🎨 THEME"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(20, 8, 20, 8)
            background = createRoundedBox(t.cardBg, t.accentColor, 2, 40f)
            setOnClickListener {
                themeIndex = (themeIndex + 1) % themes.size
                refreshBoardTheme()
            }
        }

        navBar.addView(backBtn)
        navBar.addView(arenaTag)
        navBar.addView(themeCycleBtn)
        main.addView(navBar)

        // Player Spotlight Scoreboard Row (Player X | Draws | Player O)
        val scoreboardRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 14) }
        }

        p1Card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val p1Symbol = TextView(this).apply {
            text = "X"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerXColor)
        }
        val p1NameTxt = TextView(this).apply {
            text = player1Name
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        p1ScoreLabel = TextView(this).apply {
            text = "Wins: $scoreX"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }
        p1Card.addView(p1Symbol)
        p1Card.addView(p1NameTxt)
        p1Card.addView(p1ScoreLabel)

        val drawCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 14, 12, 14)
            background = createRoundedBox(t.cardBg, t.tileBorder, 1, 18f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(10, 0, 10, 0) }
        }
        val drawTitle = TextView(this).apply {
            text = "DRAWS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.GRAY)
        }
        drawScoreLabel = TextView(this).apply {
            text = "$draws"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        drawCard.addView(drawTitle)
        drawCard.addView(drawScoreLabel)

        p2Card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val p2Symbol = TextView(this).apply {
            text = "O"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.playerOColor)
        }
        val p2NameTxt = TextView(this).apply {
            text = player2Name
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        p2ScoreLabel = TextView(this).apply {
            text = "Wins: $scoreO"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }
        p2Card.addView(p2Symbol)
        p2Card.addView(p2NameTxt)
        p2Card.addView(p2ScoreLabel)

        scoreboardRow.addView(p1Card)
        scoreboardRow.addView(drawCard)
        scoreboardRow.addView(p2Card)
        main.addView(scoreboardRow)

        // Turn Status & Move Counter Banner
        statusBanner = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 4)
        }
        moveCounterText = TextView(this).apply {
            text = "Move 0 / 16 · Connect 4 to Win"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 14)
        }
        main.addView(statusBanner)
        main.addView(moveCounterText)

        // Responsive 4x4 Grid Board
        val screenWidth = resources.displayMetrics.widthPixels
        val maxBoardPx = minOf((screenWidth * 0.86).toInt(), 380.dp())
        val cellMargin = 5.dp()
        val cellSize = (maxBoardPx - (cellMargin * 8)) / 4

        val boardContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
            background = createRoundedBox(t.cardBg, t.tileBorder, 2, 26f)
        }

        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 4
        }

        for (i in 0 until 16) {
            val cell = Button(this).apply {
                text = ""
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = createRoundedBox(t.tileBg, t.tileBorder, 2, 16f)
                isAllCaps = false
                setPadding(0, 0, 0, 0)
            }

            val params = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
            }

            grid.addView(cell, params)
            cells.add(cell)

            cell.setOnClickListener {
                if (gameMode == "SINGLE_PLAYER" && currentPlayer == "O") return@setOnClickListener
                playMove(i)
            }
        }

        boardContainer.addView(grid)
        main.addView(boardContainer)

        // Primary Controls Row
        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 22, 0, 0) }
        }

        val newRoundBtn = Button(this).apply {
            text = "NEW ROUND"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            background = createRoundedBox(t.playerXColor, t.playerXColor, 0, 18f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(0, 0, 8, 0) }
            setOnClickListener { resetRound() }
        }

        val swapStarterBtn = Button(this).apply {
            text = "SWAP STARTER"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = createRoundedBox(t.accentColor, t.accentColor, 0, 18f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(8, 0, 0, 0) }
            setOnClickListener {
                startingPlayer = if (startingPlayer == "X") "O" else "X"
                resetRound()
            }
        }

        controlsRow.addView(newRoundBtn)
        controlsRow.addView(swapStarterBtn)
        main.addView(controlsRow)

        // Secondary Controls Row (Move Log & Reset Scores)
        val utilityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 0) }
        }

        val logBtn = Button(this).apply {
            text = "📜 MOVE LOG"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = createRoundedBox(t.cardBg, t.tileBorder, 2, 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(0, 0, 8, 0) }
            setOnClickListener { showMoveLogDialog() }
        }

        val resetScoresBtn = Button(this).apply {
            text = "RESET SCORES"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.LTGRAY)
            background = createRoundedBox(t.cardBg, t.tileBorder, 2, 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(8, 0, 0, 0) }
            setOnClickListener {
                resetSessionStats()
                resetRound()
            }
        }

        utilityRow.addView(logBtn)
        utilityRow.addView(resetScoresBtn)
        main.addView(utilityRow)

        scroll.addView(main)
        setContentView(scroll)
        resetRound()
    }

    // ==================== 3. GAMEPLAY, ANIMATION & 4-IN-A-ROW DETECTION ====================
    private fun playMove(index: Int) {
        if (gameOver || board[index].isNotEmpty()) return

        val t = activeTheme
        board[index] = currentPlayer
        moveCount++

        val row = (index / 4) + 1
        val col = (index % 4) + 1
        val moverName = if (currentPlayer == "X") player1Name else player2Name
        moveLog.add("#$moveCount  $moverName ($currentPlayer)  →  Row $row, Col $col")

        val symbolColor = if (currentPlayer == "X") t.playerXColor else t.playerOColor
        cells[index].apply {
            text = currentPlayer
            setTextColor(symbolColor)
            setShadowLayer(12f, 0f, 0f, symbolColor)
            background = createRoundedBox(t.tileBg, symbolColor, 3, 16f)
            isEnabled = false
            animateTilePop(this)
        }

        moveCounterText.text = "Move $moveCount / 16 · Connect 4 to Win"

        val winningLine = findWinningLine4x4()
        if (winningLine != null) {
            gameOver = true
            val winnerName = if (currentPlayer == "X") player1Name else player2Name

            if (currentPlayer == "X") scoreX++ else scoreO++

            if (streakPlayer == currentPlayer) {
                streakCount++
            } else {
                streakPlayer = currentPlayer
                streakCount = 1
            }

            // Highlight all 4 winning tiles
            winningLine.forEach { idx ->
                cells[idx].background = createRoundedBox(t.winColor, Color.WHITE, 3, 16f)
                cells[idx].setTextColor(Color.BLACK)
                cells[idx].setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                animateTilePop(cells[idx])
            }

            val streakSuffix = if (streakCount >= 2) "  🔥 ${streakCount}x Streak!" else ""
            statusBanner.text = "🏆 $winnerName ($currentPlayer) Wins!$streakSuffix"
            statusBanner.setTextColor(t.winColor)

            disableBoard()
            updateScoreboardUI()
            return
        }

        if (board.all { it.isNotEmpty() }) {
            gameOver = true
            draws++
            streakPlayer = ""
            streakCount = 0
            statusBanner.text = "🤝 All 16 Cells Full — Draw!"
            statusBanner.setTextColor(Color.LTGRAY)

            disableBoard()
            updateScoreboardUI()
            return
        }

        // Switch active turn
        currentPlayer = if (currentPlayer == "X") "O" else "X"
        updateTurnUI()

        if (gameMode == "SINGLE_PLAYER" && currentPlayer == "O") {
            statusBanner.text = "🤖 $player2Name (O) is thinking..."
            statusBanner.setTextColor(t.playerOColor)
            handler.postDelayed({
                if (!isFinishing && !gameOver && currentPlayer == "O") {
                    computerMove4x4()
                }
            }, 350L)
        }
    }

    private fun findWinningLine4x4(): IntArray? {
        for (pattern in winPatterns4x4) {
            val a = pattern[0]
            val b = pattern[1]
            val c = pattern[2]
            val d = pattern[3]
            if (
                board[a].isNotEmpty() &&
                board[a] == board[b] &&
                board[b] == board[c] &&
                board[c] == board[d]
            ) {
                return pattern
            }
        }
        return null
    }

    // Tactical 4x4 AI Opponent
    private fun computerMove4x4() {
        var move = findLineMove("O", targetMatches = 3)
        if (move == -1) move = findLineMove("X", targetMatches = 3)
        if (move == -1) move = findLineMove("O", targetMatches = 2)
        if (move == -1) move = findLineMove("X", targetMatches = 2)
        if (move == -1) {
            move = listOf(5, 6, 9, 10).shuffled().firstOrNull { board[it].isEmpty() } ?: -1
        }
        if (move == -1) {
            move = listOf(0, 3, 12, 15).shuffled().firstOrNull { board[it].isEmpty() } ?: -1
        }
        if (move == -1) {
            move = board.indices.firstOrNull { board[it].isEmpty() } ?: -1
        }

        if (move != -1) {
            playMove(move)
        }
    }

    private fun findLineMove(symbol: String, targetMatches: Int): Int {
        val opponent = if (symbol == "X") "O" else "X"
        for (pattern in winPatterns4x4) {
            if (pattern.any { board[it] == opponent }) continue
            val matches = pattern.count { board[it] == symbol }
            val emptyCells = pattern.filter { board[it].isEmpty() }
            if (matches == targetMatches && emptyCells.isNotEmpty()) {
                return emptyCells.random()
            }
        }
        return -1
    }

    // ==================== 4. UI HELPERS & ANIMATIONS ====================
    private fun resetRound() {
        handler.removeCallbacksAndMessages(null)
        moveLog.clear()
        val t = activeTheme
        for (i in 0 until 16) {
            board[i] = ""
            cells[i].apply {
                text = ""
                isEnabled = true
                setTextColor(Color.WHITE)
                setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                background = createRoundedBox(t.tileBg, t.tileBorder, 2, 16f)
            }
        }
        moveCount = 0
        gameOver = false
        currentPlayer = startingPlayer
        moveCounterText.text = "Move 0 / 16 · Connect 4 to Win"
        updateScoreboardUI()
        updateTurnUI()

        if (gameMode == "SINGLE_PLAYER" && currentPlayer == "O") {
            statusBanner.text = "🤖 $player2Name (O) starts..."
            statusBanner.setTextColor(t.playerOColor)
            handler.postDelayed({
                if (!isFinishing && !gameOver && currentPlayer == "O") {
                    computerMove4x4()
                }
            }, 350L)
        }
    }

    private fun updateTurnUI() {
        val t = activeTheme
        val activeName = if (currentPlayer == "X") player1Name else player2Name
        val activeColor = if (currentPlayer == "X") t.playerXColor else t.playerOColor

        statusBanner.text = "$activeName ($currentPlayer)'s Turn"
        statusBanner.setTextColor(activeColor)

        p1Card.background = if (currentPlayer == "X") {
            createRoundedBox(t.cardBg, t.playerXColor, 4, 20f)
        } else {
            createRoundedBox(t.cardBg, t.tileBorder, 1, 20f)
        }

        p2Card.background = if (currentPlayer == "O") {
            createRoundedBox(t.cardBg, t.playerOColor, 4, 20f)
        } else {
            createRoundedBox(t.cardBg, t.tileBorder, 1, 20f)
        }
    }

    private fun updateScoreboardUI() {
        p1ScoreLabel.text = "Wins: $scoreX"
        p2ScoreLabel.text = "Wins: $scoreO"
        drawScoreLabel.text = "$draws"
    }

    private fun refreshBoardTheme() {
        val savedBoard = board.copyOf()
        val savedTurn = currentPlayer
        val savedGameOver = gameOver
        val savedMoveCount = moveCount
        val savedStatusText = statusBanner.text.toString()
        val savedLog = ArrayList(moveLog)

        createArenaScreen()

        moveLog.clear()
        moveLog.addAll(savedLog)

        val t = activeTheme
        for (i in 0 until 16) {
            board[i] = savedBoard[i]
            if (board[i].isNotEmpty()) {
                val color = if (board[i] == "X") t.playerXColor else t.playerOColor
                cells[i].text = board[i]
                cells[i].setTextColor(color)
                cells[i].setShadowLayer(12f, 0f, 0f, color)
                cells[i].background = createRoundedBox(t.tileBg, color, 3, 16f)
                cells[i].isEnabled = false
            }
        }
        currentPlayer = savedTurn
        gameOver = savedGameOver
        moveCount = savedMoveCount
        moveCounterText.text = "Move $moveCount / 16 · Connect 4 to Win"

        if (gameOver) {
            disableBoard()
            statusBanner.text = savedStatusText
            val winLine = findWinningLine4x4()
            if (winLine != null) {
                winLine.forEach { idx ->
                    cells[idx].background = createRoundedBox(t.winColor, Color.WHITE, 3, 16f)
                    cells[idx].setTextColor(Color.BLACK)
                }
                statusBanner.setTextColor(t.winColor)
            }
        } else {
            updateTurnUI()
        }
    }

    private fun showMoveLogDialog() {
        val content = if (moveLog.isEmpty()) {
            "No moves played yet in this round."
        } else {
            moveLog.joinToString("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("4×4 ROUND MOVE LOG")
            .setMessage(content)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun resetSessionStats() {
        scoreX = 0
        scoreO = 0
        draws = 0
        streakPlayer = ""
        streakCount = 0
        startingPlayer = "X"
    }

    private fun disableBoard() {
        cells.forEach { it.isEnabled = false }
    }

    private fun animateTilePop(view: View) {
        val anim = ScaleAnimation(
            0.45f, 1f,
            0.45f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 220L
            interpolator = OvershootInterpolator(1.6f)
        }
        view.startAnimation(anim)
    }

    private fun createRoundedBox(
        fillColor: Int,
        strokeColor: Int,
        strokeWidthDp: Int,
        radiusDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(fillColor)
            if (strokeWidthDp > 0) {
                setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), strokeColor)
            }
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
