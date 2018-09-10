package com.potrik.main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

/**
 * Chess.java
 *
 * Created by Erling Rorvik (Potrik) on 24.03.2016
 * Last updated on 09.04.2016
 */
public class Chess extends Canvas {
    /*
     * GUI:
     * The width and height of a tile
     * The buffering level for rendering
     * The window frame object
     */
    private final int width = 80;
    private final int height = 80;
    private static final int bufferingLevel = 3;
    private JFrame frame;

    /*
     * The number of squares on the board (10 * 12)
     */
    private static final byte BOARD_SQUARES = 120;

    /*
     * The maximum number of half moves in a game
     */
    private static final int MAX_GAME_MOVES = 2048;

    /*
     * The maximum number of possible moves in a position
     */
    private static final int MAX_POSITION_MOVES = 256;

    /*
     * The maximum number of depth to search
     */
    private static final int MAX_DEPTH = 64;

    /*
     * The number of primary variation entries in total for each position
     */
    private static final int PV_ENTRIES = 10000;

    /*
     * Static variable for infinity, as Integer.MAX_VALUE does not work as -MIN_VALUE = MIN_VALUE
     */
    private static final int INF = 2000000;

    /*
     * Game Status
     */
    private static final String RUNNING = "Running";
    private static final String MATE = "Check Mate";
    private static final String STALEMATE = "Stale Mate";
    private static final String DRAW = "Draw";

    /*
     * Colors
     */
    private static final byte WHITE = 0;
    private static final byte BLACK = 1;
    private static final byte NONE = 2;

    /*
     * Castle Permissions
     * The castle permission is stored in a 4 bits 0000
     * Where the last represents white king side castle (2^0)
     * Second represents white queen side castle (2^1)
     * Third represents black king side castle (2^2)
     * Fourth represents black queen side castle (2^3)
     */
    private static final byte WKCA = 1;
    private static final byte WQCA = 2;
    private static final byte BKCA = 4;
    private static final byte BQCA = 8;

    /*
     * Static enumeration of the pieces
     */
    private static final byte EMPTY = 0;
    private static final byte wP = 1;
    private static final byte wN = 2;
    private static final byte wB = 3;
    private static final byte wR = 4;
    private static final byte wQ = 5;
    private static final byte wK = 6;
    private static final byte bP = 7;
    private static final byte bN = 8;
    private static final byte bB = 9;
    private static final byte bR = 10;
    private static final byte bQ = 11;
    private static final byte bK = 12;

    /*
     * Squares values of some important squares
     * The no square value (equivalent to null)
     * The offboard value (square is not on the board)
     */
    private static final byte NO_SQ = 99;
    private static final byte OFFBOARD = 100;

    /*
     * Quick Arrays for getting the file and rank of a square
     */
    private static final byte[] getFile = new byte[BOARD_SQUARES];
    private static final byte[] getRank = new byte[BOARD_SQUARES];

    /*
     * Quick Arrays for converting between 64 base square (0 - 63) to 120 base square (0 - 120)
     */
    private static final byte[] getSquare64 = new byte[BOARD_SQUARES];
    private static final byte[] getSquare120 = new byte[64];

    /*
     * Quick Array for mirroring the base 64 board squares from white to black, so that a1 -> a8 and f3 -> f6 etc.
     */
    private static final byte[] mirror64 = {
            56, 57, 58, 59, 60, 61, 62, 63,
            48, 49, 50, 51, 52, 53, 54, 55,
            40, 41, 42, 43, 44, 45, 46, 47,
            32, 33, 34, 35, 36, 37, 38, 39,
            24, 25, 26, 27, 28, 29, 30, 31,
            16, 17, 18, 19, 20, 21, 22, 23,
            8, 9, 10, 11, 12, 13, 14, 15,
            0, 1, 2, 3, 4, 5, 6, 7
    };

    /*
     * Quick Arrays for checking a piece is of a type, regardless of color, saves the extra p == wP || p == bP.
     * Indexed by the piece number
     */
    private static final boolean[] piecePawn = {false, true, false, false, false, false, false, true, false, false, false, false, false};
    private static final boolean[] pieceKnight = {false, false, true, false, false, false, false, false, true, false, false, false, false};
    private static final boolean[] pieceBishopQueen = {false, false, false, true, false, true, false, false, false, true, false, true, false};
    private static final boolean[] pieceRookQueen = {false, false, false, false, true, true, false, false, false, false, true, true, false};
    private static final boolean[] pieceKing = {false, false, false, false, false, false, true, false, false, false, false, false, true};

    /*
     * Quick Array for getting the piece number for the king indexed by side
     */
    private static final byte[] sidesKings = {wK, bK};

    /*
     * Quick Array for getting the color of a piece, indexed by piece number
     */
    private static final int[] colPieces = {NONE, WHITE, WHITE, WHITE, WHITE, WHITE, WHITE, BLACK, BLACK, BLACK, BLACK, BLACK, BLACK};

    /*
     * Array that defines if a piece is a sliding piece or not indexed by piece number
     */
    private static final boolean[] slidePiece = {false, false, false, true, true, true, false, false, false, true, true, true, false};

    /*
     * Array that defines the value of the pieces indexed by piece number
     */
    private static final int[] valPieces = {0, 100, 325, 325, 550, 1000, 50000, 100, 325, 325, 550, 1000, 50000};

    /*
     * Array that defines the character symbols of the pieces indexed by piece number
     */
    private static final char[] pieceChars = ".PNBRQKpnbrqk".toCharArray();

    /*
     * Array that defines the character symbols of the sides indexed by side
     */
    private static final char[] sideChar = "wb-".toCharArray();

    /*
     * CastlePermission Quick Array indexed by square number base 120
     * Whenever a piece moves from or to a square, the castle permission &= castlePermSquare[squareNumber]
     * So that if either a king or rook moves or is captured, then castling is no longer possible.
     * Since castling permission is stored in 4 bits 1111 (all castling possible) & with for example 12 = 1100
     * if the white king was moved, results in a castle permission 1100 for only black can castle.
     */
    private static final byte[] castlePermSquare = {
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 13, 15, 15, 15, 12, 15, 15, 14, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 7, 15, 15, 15, 3, 15, 15, 11, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15
    };

    /*
     * The score tables for each piece, indexed by square number base 64
     * These represent the value of each piece when it's located on a given square
     */
    private static final int[] pawnScoreTable = {
            0, 0, 0, 0, 0, 0, 0, 0,
            10, 10, 0, -10, -10, 0, 10, 10,
            5, 0, 0, 5, 5, 0, 0, 5,
            0, 0, 10, 20, 20, 10, 0, 0,
            5, 5, 5, 10, 10, 5, 5, 5,
            10, 10, 10, 20, 20, 10, 10, 10,
            20, 20, 20, 30, 30, 20, 20, 20,
            0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] knightScoreTable = {
            0, -10, 0, 0, 0, 0, -10, 0,
            0, 0, 0, 5, 5, 0, 0, 0,
            0, 0, 10, 10, 10, 10, 0, 0,
            0, 0, 10, 20, 20, 10, 5, 0,
            5, 10, 15, 20, 20, 15, 10, 5,
            5, 10, 10, 20, 20, 10, 10, 5,
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] bishopScoreTable = {
            0, 0, -10, 0, 0, -10, 0, 0,
            0, 0, 0, 10, 10, 0, 0, 0,
            0, 0, 10, 15, 15, 10, 0, 0,
            0, 10, 15, 20, 20, 15, 10, 0,
            0, 10, 15, 20, 20, 15, 10, 0,
            0, 0, 10, 15, 15, 10, 0, 0,
            0, 0, 0, 10, 10, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] rookScoreTable = {
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 5, 10, 10, 5, 0, 0,
            0, 0, 5, 10, 10, 5, 0, 0,
            25, 25, 25, 25, 25, 25, 25, 25,
            0, 0, 5, 10, 10, 5, 0, 0
    };
    private static final int[] queenScoreTable = {
            0, 0, 0, -10, 0, 0, 0, 0,
            0, 0, 5, 5, 5, 0, 0, 0,
            0, 10, 2, 5, 5, 8, 0, 0,
            10, 0, 5, 10, 10, 5, 10, 0,
            0, 0, 5, 10, 10, 5, 0, 10,
            0, 0, 3, 5, 5, 3, 0, 0,
            3, 5, 7, 7, 7, 7, 5, 3,
            1, 2, 3, 4, 4, 3, 2, 1
    };

    /*
     * Static score value for the bishop pair
     */
    private static final int scoreBishopPair = 40;

    /*
     * MvvLva is Most Valuable Victim, Least Valuable Attacker.
     * Because we want a beta cutoff as early as possible, we change the search order to search the best moves first.
     * Capture moves are ranked higher than quiet moves, and they are ranked after the most valuable victim compared
     * to the least valuable attacker.
     * Array indexed by piece number for the MvvLva values of each piece.
     */
    private static final int[] MvvLvaValue = {0, 100, 200, 300, 400, 500, 600, 100, 200, 300, 400, 500, 600};

    /*
     * MvvLva capture scores for each capture indexed by piece number * piece number
     * index = captured piece * 13 + attacking piece
     * For all possible combinations of piece capturing
     * Saving computation time by having this pre-calculated
     */
    private static final int[] MvvLvaScore = new int[13 * 13];

    /*
     * Square number base 120 for the squares on the first and eighth rank
     * Used for checking castling possibilities
     */
    private static final byte A1 = 21;
    private static final byte B1 = 22;
    private static final byte C1 = 23;
    private static final byte D1 = 24;
    private static final byte E1 = 25;
    private static final byte F1 = 26;
    private static final byte G1 = 27;
    private static final byte H1 = 28;
    private static final byte A8 = 91;
    private static final byte B8 = 92;
    private static final byte C8 = 93;
    private static final byte D8 = 94;
    private static final byte E8 = 95;
    private static final byte F8 = 96;
    private static final byte G8 = 97;
    private static final byte H8 = 98;

    /*
     * The directions each of the pieces can move in
     */
    private static final byte[] dirKn = {-8, -19, -21, -12, 8, 19, 21, 12};
    private static final byte[] dirRk = {-1, -10, 1, 10};
    private static final byte[] dirBi = {-9, -11, 11, 9};
    private static final byte[] dirKi = {-1, -10, 1, 10, -9, -11, 11, 9};

    /*
     * Array indexed by piece number that gets the direction each piece can move in.
     * The King and Queen can move in the same directions, except the Queen is a sliding piece
     */
    private static final byte[][] pieceDir = {{}, {}, dirKn, dirBi, dirRk, dirKi, dirKi, {}, dirKn, dirBi, dirRk, dirKi, dirKi};

    /*
     * The score of a check mate
     */
    private static final int MATE_SCORE = 1000000;

    /*
     * Move variables
     *
     * Move structure, 32 bit int
     *
     * 0000 0000 0000 0000 0000 0000 0000 0000
     *                                 xx xxxx  - From square    (0 - 63)
     *                          xxxx xx         - To square      (0 - 63)
     *                     xxxx                 - Captured Piece (0 - 15)
     *                xxxx                      - Promoted Piece (0 - 15)
     *              x                           - En passant     (boolean)
     *             x                            - Pawn start     (boolean)
     *            x                             - Castle move    (boolean)
     *
     * You can AND with the the move flags to get the value of each part
     * of the move integer
     */
    private static final int MFLAG_ENPASSANT = 0x100000;
    private static final int MFLAG_PAWNSTART = 0x200000;
    private static final int MFLAG_CASTLE = 0x400000;
    private static final int MFLAG_CAPTURE = 0xF000;
    private static final int MFLAG_PROMOTION = 0xF0000;

    /*
     * Array containing the images of each of the pieces indexed by piece number
     */
    private BufferedImage[] imagePieces;

    /*
     * The main board array, 120 tiles where the piece numbers are stored
     */
    private byte[] board;

    /*
     * The current side to move
     */
    private byte side;

    /*
     * The number of moves since last capture or pawn move.
     * If this number reaches 100 it is a draw
     */
    private byte fiftyMove;

    /*
     * The total number of half moves (ply) this game
     */
    private int numPly;

    /*
     * The number of half moves calculated - the current depth
     */
    private int comPly;

    /*
     * The en passant square
     */
    private byte enPas;

    /*
     * The castle permissions stored as bits instead of a boolean
     * This allows for much simpler castle permission calculations by
     * permission &= the castle permission change for this square instead
     * of having a whole bunch of if-statements and booleans
     * 0000 0000
     *         x - White king side castle
     *        x  - White queen side castle
     *       x   - Black king side castle
     *      x    - Black queen side castle
     */
    private byte castlePerm;

    /*
     * The material value of the two sides indexed by side
     */
    private int[] material;

    /*
     * The number of pieces of each piece type, indexed by square
     */
    private byte[] numPieces;

    /*
     * The square of each piece on the board.
     * index = piece * 10 + the number of this piece for this piece type
     * The maximum possible pieces of a type is 10 (for knight, rooks and bishops, if 8 pawns promote)
     */
    private byte[] pieceListSquare;

    /*
     * The hash position key of the current position.
     * The hash key system is to check for position repetition.
     * Each position has its own unique hash key, and if the exact same position
     * occurs again it will have the same hash key.
     * Thus things like the castle permissions and side must be included in the hash,
     * because if they are changed it is not the same position.
     */
    private long posKey;

    /*
     * The unique hash keys for each combination of piece and square
     * 13 * 120 size, index = piece num * 120 + square
     */
    private int[] pieceKeys;

    /*
     * The unique hash keys for the castle permissions,
     * 16 size, index = castle permission
     */
    private int[] castleKeys;

    /*
     * The unique side hash key
     */
    private int sideKey;

    /*
     * The history of position keys this game
     */
    private long[] historyPosKey;

    /*
     * The history of moves this game
     */
    private int[] historyMove;

    /*
     * The history of the fifty move status this game
     */
    private byte[] historyFiftymove;

    /*
     * The history of the en passant square this game
     */
    private byte[] historyEnPas;

    /*
     * The history of the castle permission this game
     */
    private byte[] historyCastlePerm;

    /*
     * The primary variation is stored in two arrays,
     * The move in pvMove indexed by position key % PV_ENTRIES
     * Because two different position keys may give the same index, the array
     * pvPosition Key keeps track of the original position key the pv entry came from
     */
    private long[] primaryVariationPositionKey;
    private int[] primaryVariationMove;

    /*
     * The primary variation array stores the move integer of the primary variation
     * down two the depth, indexed by the ply number
     */
    private int[] primaryVariation;

    /*
     * Array that stores the move score of good quiet moves that are good in most of the situations.
     * This move will most likely occur in many situations and need to be searched earlier.
     * Indexed by the from and to square
     */
    private int[] searchHistory;

    /*
     * Array of killer moves, moves that are rated the highest, indexed by the depth.
     * There are always two killer moves stored. The killer move is a good move that might occur
     * in several variations, therefore it should be searched first.
     */
    private int[] searchKillers;

    /*
     * Array to store the move ordering scores of each move indexed by the move list index
     */
    private int[] moveScores;

    /*
     * The move list is the list of moves available in a position.
     * The moveListStart array keeps track of the starting index for each ply of the available moves
     * in the move list array. moveListStart is indexed by the ply number
     */
    private int[] moveList;
    private int[] moveListStart;

    /*
     * The number of nodes searched
     */
    private long searchnodes;

    /*
     * The number of beta cut offs
     * and the number of beta cut offs on the first move.
     * These provide statistics for the move ordering. We want to search the best moves first,
     * So that we have the highest chance of a beta cut off.
     */
    private int searchbc;
    private int searchbcf;

    /*
     * The search depth to search to
     */
    private int searchdepth;

    /*
     * Boolean to keep track of the searching, if it should continue to search
     */
    private boolean searching;

    /*
     * The current status of the game
     */
    private String gameStatus;

    /*
     * Boolean to show the gui, if loading resources failed, hide the gui
     */
    private boolean gui;

    /*
     * The selected tile by the user
     */
    private int sx, sy;

    /*
     * Boolean to check if the player is allowed to make a move
     */
    private boolean playerMove;

    /*
     * The thread to handle rendering
     */
    private ProcessThread renderingThread;

    /**
     * Init the arrays to get the rank and file of a tile
     */
    private static void initGetRankFile() {
        for (int i = 0; i < BOARD_SQUARES; i++) {
            /*
             * Loop through all the tiles and set them to offboard
             */
            getFile[i] = OFFBOARD;
            getRank[i] = OFFBOARD;
        }

        for (byte r = 0; r < 8; r++) {
            for (byte f = 0; f < 8; f++) {
                /*
                 * Loop through all the ranks and files,
                 * calculate the square base 64, set the rank to r
                 * and set the file to f
                 */
                int sq = (21 + f) + (10 * r);
                getFile[sq] = f;
                getRank[sq] = r;
            }
        }
    }

    /**
     * Init the arrays for converting between square base 120 and base 64
     */
    private static void initGetSquare() {
        for (int i = 0; i < BOARD_SQUARES; i++) {
            /*
             * Loop through all the squares and set them to 64 (offboard)
             */
            getSquare64[i] = 64;
        }

        for (int i = 0; i < 64; i++) {
            /*
             * Loop through all the squares and set them to 120 (offboard)
             */
            getSquare120[i] = 120;
        }

        byte sq64 = 0;
        for (byte r = 0; r < 8; r++) {
            for (byte f = 0; f < 8; f++) {
                /*
                 * Loop through all the files and ranks, get the square number base 120 and set the square 64 number.
                 */
                byte sq = (byte) ((21 + f) + (10 * r));
                getSquare120[sq64] = sq;
                getSquare64[sq] = sq64;
                sq64++;
            }
        }
    }

    /**
     * Init the array for getting the MvvLva score for each piece combination
     */
    private static void initMvvLva() {
        for (byte a = wP; a <= bK; a++) {
            for (byte v = wP; v < bK; v++) {
                /*
                 * Loop through all the attacking pieces and set the values.
                 * The value is calculated by taking the MvvLva value of the victim, add 6 (because 600/100 is 6)
                 * and subtract the value of the attacker divided by 100.
                 * This way the moves sorted by score will be most valuable victim first,
                 * then those are sorted by least valuable attacker.
                 */
                MvvLvaScore[v * 13 + a] = MvvLvaValue[v] + 6 - (MvvLvaValue[a] / 100);
            }
        }
    }

    /**
     * Main method for the program.
     * Contains the main program control system
     *
     * @param args no runtime arguments
     */
    public static void main(String[] args) {
        /*
         * First set up the basic arrays
         */
        initGetRankFile();
        initGetSquare();
        initMvvLva();

        /*
         * Input setup
         */
        Scanner input = new Scanner(System.in);
        boolean reading = true;

        /*
         * Print the instructions
         */
        System.out.println("Erling's Chess Engine");
        System.out.println("Commands:");
        System.out.println("  setboard <fen> set the board to the given FEN");
        System.out.println("  search <depth> search the position");
        System.out.println("  move <a1h8>    move a piece from sq to sq and if promotion add the piece char to the end");
        System.out.println("  revertmove     revert the move");
        System.out.println("  makeai <depth> make the ai do the best move");
        System.out.println("  run <depth>    run an ai vs ai game");
        System.out.println("  new            set the board to the start position");
        System.out.println("  stop           stop the process running");
        System.out.println("  play <depth>   play against the computer");
        System.out.println("  quit / exit    exit the application");

        /*
         * Setup the chess object to the starting position
         */
        Chess chess = new Chess();
        chess.parseFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        chess.printBoard();

        /*
         * Declare the process thread
         */
        ProcessThread processThread = null;

        while (reading) {
            /*
             * While we are reading instructions from the program
             */
            String line = input.nextLine();
            if (line.startsWith("quit") || line.startsWith("exit")) {
                /*
                 * End the loop
                 */
                reading = false;
            } else if (line.startsWith("search ")) {
                if (processThread != null && processThread.isAlive()) {
                    System.out.println("You already have a process running!");
                } else {
                    try {
                        /*
                         * Try to read the depth and search the position in a new thread
                         */
                        final int d = Integer.parseInt(line.substring(7));
                        if (d > 0) {
                            processThread = new ProcessThread() {
                                public void run() {
                                    chess.searchPosition(1, d);
                                }
                            };
                            processThread.start();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid Number!");
                    }
                }
            } else if (line.startsWith("setboard ")) {
                try {
                    /*
                     * Try to read the fen and parse it
                     */
                    String fen = line.substring(9);
                    chess.parseFen(fen);
                    chess.printBoard();
                } catch (StringIndexOutOfBoundsException e) {
                    System.out.println("Invalid FEN!");
                }
            } else if (line.startsWith("move ")) {
                try {
                    /*
                     * Try to read, parse, and execute the move
                     */
                    String move = line.substring(5);
                    if (chess.attemptMove(move)) {
                        if (!chess.checkStatus()) {
                            chess.frame.setTitle(chess.gameStatus);
                        }
                        chess.printBoard();
                    } else {
                        System.out.println("Invalid move!");
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    System.out.println("Invalid move format!");
                }
            } else if (line.startsWith("revertmove")) {
                chess.revertMove();
                chess.printBoard();
            } else if (line.startsWith("makeai ")) {
                if (processThread != null && processThread.isAlive()) {
                    System.out.println("You already have a process running!");
                } else {
                    try {
                        /*
                         * Try to read the depth, search the position in an new thread,
                         * find the best move and execute it.
                         */
                        final int d = Integer.parseInt(line.substring(7));
                        if (d > 0) {
                            processThread = new ProcessThread() {
                                public void run() {
                                    int move = chess.searchPosition(d, d);
                                    if (move != 0) chess.makeMove(move);
                                    if (!chess.checkStatus()) {
                                        chess.frame.setTitle(chess.gameStatus);
                                    }
                                    chess.printBoard();
                                }
                            };
                            processThread.start();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid Number!");
                    }
                }
            } else if (line.startsWith("run ")) {
                if (processThread != null && processThread.isAlive()) {
                    System.out.println("You already have a process running!");
                } else {
                    try {
                        /*
                         * Run a game between two computers searching at a depth.
                         * Try to read the depth, and while the game is running, and there
                         * are available moves, and the game has not ended, search the position in a new thread,
                         * find the best move and execute it.
                         */
                        final int d = Integer.parseInt(line.substring(4));
                        if (d > 0) {
                            processThread = new ProcessThread() {
                                public void run() {
                                    running = true;
                                    long start = System.currentTimeMillis();
                                    int move = chess.searchPosition(d, d);
                                    while (move != 0 && running) {
                                        chess.makeMove(move);
                                        if (!chess.checkStatus()) {
                                            /*
                                             * The game has ended in some way
                                             */
                                            chess.frame.setTitle(chess.gameStatus);
                                            break;
                                        }
                                        chess.printBoard();
                                        move = chess.searchPosition(d, d);
                                    }
                                    long end = System.currentTimeMillis();
                                    if (!chess.checkStatus()) {
                                        chess.frame.setTitle(chess.gameStatus);
                                    }
                                    chess.printBoard();
                                    System.out.println("Time Used: " + (end - start) + " ms");
                                }
                            };
                            processThread.start();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid Number!");
                    }
                }
            } else if (line.startsWith("new")) {
                /*
                 * Set the board position to the startup position
                 */
                chess.parseFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                chess.checkStatus();
                chess.printBoard();
            } else if (line.startsWith("play ")) {
                try {
                    /*
                     * Play a game against the computer searching at a given depth.
                     * Try to read the depth, define the side of the computer, and
                     * while the process is running, there are available moves, and it has not ended
                     * in any way, search the position to find the best move and execute it.
                     */
                    final int d = Integer.parseInt(line.substring(5));
                    if (d > 0) {
                        processThread = new ProcessThread() {
                            public void run() {
                                running = true;
                                byte side = chess.side;
                                while (running) {
                                    if (chess.side == side) {
                                        /*
                                         * Check if it's the computer's turn to move
                                         */

                                        int move = chess.searchPosition(d, d);
                                        if (move != 0) {
                                            chess.makeMove(move);
                                            if (!chess.checkStatus()) {
                                                chess.frame.setTitle(chess.gameStatus);
                                                break;
                                            }
                                            chess.printBoard();
                                        } else {
                                            if (!chess.checkStatus()) {
                                                chess.frame.setTitle(chess.gameStatus);
                                            }
                                            break;
                                        }
                                        if (!chess.checkStatus()) {
                                            chess.frame.setTitle(chess.gameStatus);
                                            break;
                                        }
                                    }

                                    try {
                                        /*
                                         * There is no need to check the turn constantly to see if the player
                                         * has made a move. Wait for a few milliseconds.
                                         */
                                        Thread.sleep(25);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };
                        processThread.start();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid Number!");
                }
            } else if (line.startsWith("stop")) {
                if (processThread == null) {
                    System.out.println("No processes running!");
                } else {
                    /*
                     * Stop the process from running and end the search.
                     * Join the thread.
                     */
                    processThread.running = false;
                    chess.searching = false;
                    try {
                        processThread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Invalid command!");
            }
        }

        /*
         * Stop the program
         */
        chess.stop();
    }

    /**
     * Chess object initialization
     */
    private Chess() {
        /*
         * See comments above for variable explanation
         */
        board = new byte[BOARD_SQUARES];
        side = 0;
        fiftyMove = 0;
        numPly = 0;
        comPly = 0;
        enPas = NO_SQ;
        castlePerm = 0;
        material = new int[2];
        numPieces = new byte[13];
        pieceListSquare = new byte[13 * 10];
        posKey = 0;
        searchnodes = 0;
        searchbc = 0;
        searchbcf = 0;
        searchdepth = 0;
        pieceKeys = new int[13 * 120];
        castleKeys = new int[16];
        primaryVariationPositionKey = new long[PV_ENTRIES];
        primaryVariationMove = new int[PV_ENTRIES];
        moveList = new int[MAX_DEPTH * MAX_POSITION_MOVES];
        moveScores = new int[MAX_DEPTH * MAX_POSITION_MOVES];
        moveListStart = new int[MAX_DEPTH];
        historyPosKey = new long[MAX_GAME_MOVES];
        historyFiftymove = new byte[MAX_GAME_MOVES];
        historyEnPas = new byte[MAX_GAME_MOVES];
        historyCastlePerm = new byte[MAX_GAME_MOVES];
        historyMove = new int[MAX_GAME_MOVES];
        primaryVariation = new int[MAX_DEPTH];
        searchHistory = new int[13 * BOARD_SQUARES];
        searchKillers = new int[2 * MAX_DEPTH];
        gameStatus = RUNNING;
        gui = true; // gui is set to true by default, but will turn off if resource loading fails
        sx = -1;
        sy = -1;
        playerMove = true;

        /*
         * Init the position hash key variables to random positive integers
         */
        Random random = new Random();
        for (int i = 0; i < pieceKeys.length; i++) {
            pieceKeys[i] = Math.abs(random.nextInt());
        }
        for (int i = 0; i < castleKeys.length; i++) {
            castleKeys[i] = Math.abs(random.nextInt());
        }
        sideKey = Math.abs(random.nextInt());

        try {
            /*
             * Try to load the image resources for the pieces in the gui
             */
            imagePieces = new BufferedImage[]{
                    null,
                    ImageIO.read(getClass().getResource("/white/pawn.png")),
                    ImageIO.read(getClass().getResource("/white/knight.png")),
                    ImageIO.read(getClass().getResource("/white/bishop.png")),
                    ImageIO.read(getClass().getResource("/white/rook.png")),
                    ImageIO.read(getClass().getResource("/white/queen.png")),
                    ImageIO.read(getClass().getResource("/white/king.png")),
                    ImageIO.read(getClass().getResource("/black/pawn.png")),
                    ImageIO.read(getClass().getResource("/black/knight.png")),
                    ImageIO.read(getClass().getResource("/black/bishop.png")),
                    ImageIO.read(getClass().getResource("/black/rook.png")),
                    ImageIO.read(getClass().getResource("/black/queen.png")),
                    ImageIO.read(getClass().getResource("/black/king.png"))
            };
        } catch (IOException e) {
            /*
             * If the loading failed, disable the gui
             */
            gui = false;
            e.printStackTrace();
        }

        if (gui) {
            /*
             * If gui is turned on, initialize it
             */
            Dimension size = new Dimension(width * 8, height * 8);
            frame = new JFrame();
            setPreferredSize(size);
            frame.setResizable(false);
            frame.setTitle("Chess");
            frame.add(this);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (playerMove) {
                        /*
                         * If the player is allowed to make a move, get the tile,
                         * check if any other tiles are selected, if they are attempt a move
                         * unless they are the same, in either case un-mark the tile.
                         * Otherwise, mark the tile
                         */
                        int tx = e.getX() / width;
                        int ty = e.getY() / height;

                        if (sy >= 0 && sx >= 0) {
                            if (tx != sx || ty != sy) {
                                String moveString = "" + (char) ('a' + sx) + (8 - sy) + (char) ('a' + tx) + (8 - ty);
                                System.out.println("Attempting Move : " + moveString);
                                boolean res = attemptMove(moveString);
                                if (!res) {
                                    System.out.println("Move failed!");
                                }
                            }
                            sx = -1;
                            sy = -1;
                        } else {
                            sy = ty;
                            sx = tx;
                        }
                        checkStatus(); // Check the status to see if the game has ended or not
                    }
                }
            });
            /*
             * Because the rendering thread must be shut down before closing the window,
             * A custom window closing event is necessary to close the chess object.
             */
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    stop();
                }
            });
            requestFocus();

            renderingThread = new ProcessThread() {
                public void run() {
                    running = true;
                    while (running) {
                        /*
                         * While the renderer is running and the game is not searching render the board
                         */
                        if (!searching) {
                            render();
                        }
                        try {
                            /*
                             * There is no need to consume processing power by rendering all the time,
                             * so sleep a few milliseconds between each time
                             */
                            Thread.sleep(1000 / 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            renderingThread.start();
        }

        resetBoard();
    }

    /**
     * Stop the gui and renderer from running
     */
    private void stop() {
        try {
            if (gui) {
                gui = false;
                renderingThread.running = false;
                renderingThread.join();
                frame.dispose();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Render the board to the screen
     */
    private void render() {
        /*
         * Create the buffer strategy and graphics if they do not exist
         */
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(bufferingLevel);
            return;
        }
        Graphics g = bs.getDrawGraphics();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                /*
                 * Loop through all the tiles on the board. If it's an odd tile color it blue, otherwise white
                 */
                if ((x + y) % 2 == 1) {
                    g.setColor(new Color(51, 188, 255));
                } else {
                    g.setColor(Color.WHITE);
                }

                /*
                 * Draw a rectangle on the square
                 */
                g.fillRect(x * width, y * height, width, height);

                /*
                 * Get the piece on the square and draw it if it is non zero
                 */
                byte piece = board[getSquare120[mirror64[x + y * 8]]]; // We mirror because the board is rendered with black on top
                if (piece != 0) {
                    g.drawImage(imagePieces[piece], x * width, y * height, width, height, null);
                }
            }
        }

        /*
         * If a square is selected mark it with a red frame
         */
        if (sx >= 0 && sy >= 0) {
            g.setColor(Color.RED);
            g.drawRect(sx * width, sy * height, width - 1, height - 1);
            g.drawRect(sx * width + 1, sy * height + 1, width - 3, height - 3);
        }

        g.dispose();
        bs.show();
    }

    /**
     * Print the board to the console
     */
    private void printBoard() {
        /*
         * Whenever we call the printBoard function we also want to show the board on the screen, in case
         * it is missed by the rendering window in between searches
         */
        if (gui) render();

        /*
         * Print the board status variables
         */
        System.out.println();
        System.out.println("Move number: " + numPly);
        System.out.println("Castling: " + castlePerm);
        System.out.print("Position: ");
        System.out.printf("%02x\n", posKey);
        System.out.println("En passant: " + enPas);
        System.out.println("50 move status: " + fiftyMove);
        System.out.println("Side : " + sideChar[side]);
        System.out.println("Status: " + gameStatus);
        System.out.println();
        System.out.println("    a  b  c  d  e  f  g  h");
        System.out.println();

        /*
         * Print the symbols, tiles and pieces
         */
        for (byte rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + "  ");
            for (byte file = 0; file < 8; file++) {
                System.out.print(" " + pieceChars[board[(21 + file) + (10 * rank)]] + " ");
            }
            System.out.println("  " + (rank + 1));
        }
        System.out.println();
        System.out.println("    a  b  c  d  e  f  g  h");
        System.out.println();
    }

    /**
     * Create a printable string from a move of integer format
     *
     * @param move Move variable of integer format
     * @return String object in printable format of the move
     */
    private String moveToString(int move) {
        String result = "";

        /*
         * Get the to and from files and ranks and add them to the result
         */
        int from_file = getFile[getFromSq(move)];
        int from_rank = getRank[getFromSq(move)];
        int to_file = getFile[getToSq(move)];
        int to_rank = getRank[getToSq(move)];

        result += ((char) ('a' + from_file)) + "" + (1 + from_rank) + ((char) ('a' + to_file)) + "" + (1 + to_rank);

        /*
         * If the move was a promotion or capture move add the promoted or captured piece to the result
         */
        int promoted = getPromotedPiece(move);
        int captured = getCapturedPiece(move);

        if (captured != 0) {
            result += "x" + pieceChars[captured];
        }
        if (promoted != 0) {
            result += "=" + pieceChars[promoted];
        }

        return result;
    }

    /**
     * Generate the position hash key for the current board position
     *
     * @return long position hash key
     */
    private long generatePosKey() {
        long finalKey = 0;

        /*
         * Loop through the board and hash in the piece and square combinations
         */
        for (byte sq = 0; sq < BOARD_SQUARES; sq++) {
            byte piece = board[sq];
            if (piece != EMPTY && piece != OFFBOARD) {
                finalKey ^= pieceKeys[piece * 120 + sq];
            }
        }

        /*
         * Hash in the castle permissions
         */
        finalKey ^= castleKeys[castlePerm];

        /*
         * Hash in the side key if the side is white
         */
        if (side == WHITE) {
            finalKey ^= sideKey;
        }

        /*
         * Hash in the en passant square if it exists
         */
        if (enPas != NO_SQ) {
            finalKey ^= pieceKeys[enPas];
        }

        return finalKey;
    }

    /**
     * Toggle the hash key for a piece on a square
     *
     * @param sq position of the piece by square number base 120
     * @param piece piece number of the piece to hash
     */
    private void togglePieceHash(int sq, int piece) {
        posKey ^= pieceKeys[piece * 120 + sq];
    }

    /**
     * Toggle the hash key for the castle permission
     */
    private void toggleCastleHash() {
        posKey ^= castleKeys[castlePerm];
    }

    /**
     * Toggle the hash key for the en passant square
     */
    private void toggleEnpasHash() {
        posKey ^= pieceKeys[enPas];
    }

    /**
     * Remove a piece from a square on the board
     *
     * @param sq the square base 120 from which to remove a piece
     */
    private void removePiece(byte sq) {
        /*
         * Get the piece number and the color
         */
        byte piece = board[sq];
        int color = colPieces[piece];

        /*
         * Toggle the hash for this piece on this square
         */
        togglePieceHash(sq, piece);

        /*
         * Set the square to empty and subtract from the material score
         */
        board[sq] = EMPTY;
        material[color] -= valPieces[piece];

        /*
         * Remove the piece from the piece list by searching through it until the piece is found,
         * and then swapping it with the last piece in the index, and decrementing the number of pieces
         */
        int position = -1;
        for (int i = piece * 10; i < piece * 10 + numPieces[piece]; i++) {
            if (pieceListSquare[i] == sq) {
                position = i;
                break;
            }
        }
        pieceListSquare[position] = pieceListSquare[piece * 10 + --numPieces[piece]];
    }

    /**
     * Add a piece to the board
     * @param sq the target square base 120
     * @param piece the chosen piece number
     */
    private void addPiece(byte sq, byte piece) {
        /*
         * Get the color of the piece, toggle the hash key, set the target square to the piece,
         * Update the material value and the piece list
         */
        int color = colPieces[piece];
        togglePieceHash(sq, piece);
        board[sq] = piece;
        material[color] += valPieces[piece];
        pieceListSquare[piece * 10 + numPieces[piece]++] = sq;
    }

    /**
     * Move a piece from square a to b
     * @param sq the from square base 120
     * @param to_sq the to swuare base 120
     */
    private void movePiece(byte sq, byte to_sq) {
        /*
         * Get the piece and toggle the hash keys for the squares.
         * Update the board data and change the square in the piece list
         */
        byte piece = board[sq];
        togglePieceHash(sq, piece);
        togglePieceHash(to_sq, piece);
        board[sq] = EMPTY;
        board[to_sq] = piece;
        for (int i = piece * 10; i < piece * 10 + numPieces[piece]; i++) {
            if (pieceListSquare[i] == sq) {
                pieceListSquare[i] = to_sq;
                break;
            }
        }
    }

    /**
     * Make a move
     * @param move move of integer format
     * @return boolean if the move was successful/legal or not
     */
    private boolean makeMove(int move) {
        /*
         * Get the from and to squares, and store the side to move
         */
        byte from = getFromSq(move);
        byte to = getToSq(move);
        byte mside = this.side;

        /*
         * Save the position key in the history array
         */
        historyPosKey[numPly] = posKey;

        /*
         * If the move was an en passant capture remove the captured piece
         */
        if ((move & MFLAG_ENPASSANT) != 0) {
            removePiece(mside == WHITE ? ((byte) (to - 10)) : ((byte) (to + 10)));
        } else if ((move & MFLAG_CASTLE) != 0) {
            /*
             * If the move was a castling move update the rook position based on the to square of the king
             */
            switch (to) {
                case C1:
                    movePiece(A1, D1);
                    break;
                case C8:
                    movePiece(A8, D8);
                    break;
                case G1:
                    movePiece(H1, F1);
                    break;
                case G8:
                    movePiece(H8, F8);
                    break;
                default:
                    System.out.println("Notice: Invalid castle move: " + move);
                    break;
            }
        }

        /*
         * If the previous en passant square existed toggle the hash
         * Toggle the castle permission hash
         */
        if (enPas != NO_SQ) toggleEnpasHash();
        toggleCastleHash();

        /*
         * Save the board variables to history
         */
        historyCastlePerm[numPly] = castlePerm;
        historyEnPas[numPly] = enPas;
        historyFiftymove[numPly] = fiftyMove;
        historyMove[numPly] = move;

        /*
         * Update the castle permissions based on the to and from square
         * (Check the comment for the castlePermSquare array)
         * Reset the en passant square, and toggle the castle hash
         */
        castlePerm &= castlePermSquare[from];
        castlePerm &= castlePermSquare[to];
        enPas = NO_SQ;
        toggleCastleHash();

        /*
         * Get the captured piece, and remove it if it was non empty
         * A capture resets the fifty move counter.
         */
        byte captured = getCapturedPiece(move);
        if (captured != EMPTY) {
            removePiece(to);
            fiftyMove = 0;
        } else {
            fiftyMove++;
        }

        /*
         * Increment the number of moves
         */
        numPly++;
        comPly++;

        /*
         * If the piece moved was a pawn, and it was a pawn start move, add the en passant square and toggle the hash
         */
        if (piecePawn[board[from]]) {
            fiftyMove = 0;
            if ((move & MFLAG_PAWNSTART) != 0) {
                enPas = mside == WHITE ? (byte) (from + 10) : (byte) (from - 10);
                toggleEnpasHash();
            }
        }

        /*
         * Get the promoted piece, and if it is non empty remove the pawn and add the promoted piece
         */
        byte promoted = getPromotedPiece(move);
        if (promoted != EMPTY) {
            removePiece(from);
            addPiece(to, promoted);
        } else {
            movePiece(from, to);
        }

        /*
         * Toggle the side, and toggle the side hash
         */
        side ^= 1;
        posKey ^= sideKey;

        /*
         * If the king is in check however the move is illegal. Revert the move and return false
         */
        if (isAttacked(pieceListSquare[sidesKings[mside] * 10], side)) {
            revertMove();
            return false;
        }

        return true;
    }

    /**
     * Check the current status of the board
     * @return boolean if the game is still running
     */
    private boolean checkStatus() {
        /*
         * If the fifty move rule has been reach it's a draw
         */
        if (fiftyMove >= 100) {
            gameStatus = DRAW + " - 50 moves";
            return false;
        }

        /*
         * If the same position has been repeated 3 times it's a draw
         */
        int reps = 0;
        for (int i = numPly - 1; i >= 0; i--) {
            if (historyPosKey[i] == posKey) reps++;
        }
        if (reps >= 3) {
            gameStatus = DRAW + " - Repetitions";
            return false;
        }

        /*
         * If there is insufficient mating material it's a draw
         */
        if (!(numPieces[wP] > 0 || numPieces[bP] > 0 || numPieces[wR] > 0 || numPieces[bR] > 0 || numPieces[wQ] > 0 || numPieces[bQ] > 0 || numPieces[wB] + numPieces[wN] >= 2 || numPieces[bB] + numPieces[bN] >= 2)) {
            gameStatus = DRAW;
            return false;
        }

        /*
         * If there are no legal moves the game is over, and is mate if the king is attacked, otherwise a stale mate
         */
        generateMoves();
        int legalMoves = 0;

        for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
            if (makeMove(moveList[i])) {
                legalMoves++;
                revertMove();
            }
        }
        if (legalMoves == 0) {
            if (isAttacked(pieceListSquare[sidesKings[side] * 10], side ^ 1)) {
                gameStatus = MATE;
            } else {
                gameStatus = STALEMATE;
            }
            return false;
        }

        return true;
    }

    /**
     * Revert the last move
     */
    private void revertMove() {
        /*
         * Decrement the move counter
         */
        numPly--;
        comPly--;
        if (comPly < 0) comPly = 0;
        if (numPly < 0) numPly = 0;

        /*
         * Retrieve the move from history
         */
        int move = historyMove[numPly];

        /*
         * The rest works just like makeMove()
         */
        byte to = getToSq(move);
        byte from = getFromSq(move);
        if (enPas != NO_SQ) toggleEnpasHash();
        toggleCastleHash();

        castlePerm = historyCastlePerm[numPly];
        fiftyMove = historyFiftymove[numPly];
        enPas = historyEnPas[numPly];

        if (enPas != NO_SQ) toggleEnpasHash();
        toggleCastleHash();
        side ^= 1;
        posKey ^= sideKey;

        if ((move & MFLAG_ENPASSANT) != 0) {
            if (side == WHITE) {
                addPiece((byte) (to - 10), bP);
            } else {
                addPiece((byte) (to + 10), wP);
            }
        } else if ((move & MFLAG_CASTLE) != 0) {
            switch (to) {
                case C1:
                    movePiece(D1, A1);
                    break;
                case C8:
                    movePiece(D8, A8);
                    break;
                case G1:
                    movePiece(F1, H1);
                    break;
                case G8:
                    movePiece(F8, H8);
                    break;
                default:
                    System.out.println("Notice: Invalid castle move: " + move);
                    break;
            }
        }

        movePiece(to, from);
        byte captured = getCapturedPiece(move);
        if (captured != EMPTY) {
            addPiece(to, captured);
        }

        byte promoted = getPromotedPiece(move);
        if (promoted != EMPTY) {
            removePiece(from);
            addPiece(from, colPieces[promoted] == WHITE ? wP : bP);
        }
    }

    /**
     * Reset the board
     */
    private void resetBoard() {
        resetPieceListMaterial();

        /*
         * Set all the squares to off board and fill them with empty pieces
         */
        for (int i = 0; i < BOARD_SQUARES; i++) {
            board[i] = OFFBOARD;
        }
        for (int i = 0; i < 64; i++) {
            board[getSquare120[i]] = EMPTY;
        }

        /*
         * Reset the board variables and empty the arrays.
         * See comment by declaration for more info.
         */
        side = NONE;
        enPas = NO_SQ;
        fiftyMove = 0;
        comPly = 0;
        numPly = 0;
        castlePerm = 0;
        posKey = 0;
        moveListStart[comPly] = 0;
        historyPosKey = new long[MAX_GAME_MOVES];
        historyFiftymove = new byte[MAX_GAME_MOVES];
        historyEnPas = new byte[MAX_GAME_MOVES];
        historyCastlePerm = new byte[MAX_GAME_MOVES];
        historyMove = new int[MAX_GAME_MOVES];
        gameStatus = RUNNING;
    }

    /**
     * Reset the piece list and the material values
     */
    private void resetPieceListMaterial() {
        /*
         * Loop through all the squares in the piece list and set them to no square.
         * Set the material score to 0 and the number of pieces to 0.
         */
        for (int i = 0; i < pieceListSquare.length; i++) {
            pieceListSquare[i] = NO_SQ;
        }

        for (int i = 0; i < material.length; i++) {
            material[i] = 0;
        }

        for (int i = 0; i < numPieces.length; i++) {
            numPieces[i] = 0;
        }
    }

    /**
     * Update the piece lists and material value
     */
    private void updatePieceListMaterial() {
        resetPieceListMaterial();

        /*
         * Loop through the squares of the board, and if it's a piece add it to the lists
         */
        for (int i = 0; i < 64; i++) {
            byte piece = board[getSquare120[i]];
            if (piece != EMPTY) {
                int col = colPieces[piece];
                material[col] += valPieces[piece];
                pieceListSquare[piece * 10 + numPieces[piece]] = getSquare120[i];
                numPieces[piece]++;
            }
        }
    }

    /**
     * Parse a fen position string
     * @param fen position string
     */
    private void parseFen(String fen) {
        resetBoard();

        /*
         * The reading starts at the 8th rank (index 7) and the first file (index 0)
         */
        int rank = 7;
        int file = 0;
        int fenc = 0; // The fen character position counter

        while (rank >= 0 && fenc < fen.length()) {
            int count = 1;
            byte piece;
            /*
             * If it's a piece char add it,
             * A number means x blank spaces so the piece is set to blank,
             * and set the count to the number
             * '/' or ' ' means to end the line and decrement the rank
             */
            switch (fen.charAt(fenc)) {
                case 'p':
                    piece = bP;
                    break;
                case 'r':
                    piece = bR;
                    break;
                case 'n':
                    piece = bN;
                    break;
                case 'b':
                    piece = bB;
                    break;
                case 'k':
                    piece = bK;
                    break;
                case 'q':
                    piece = bQ;
                    break;
                case 'P':
                    piece = wP;
                    break;
                case 'R':
                    piece = wR;
                    break;
                case 'N':
                    piece = wN;
                    break;
                case 'B':
                    piece = wB;
                    break;
                case 'K':
                    piece = wK;
                    break;
                case 'Q':
                    piece = wQ;
                    break;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                    piece = EMPTY;
                    count = fen.charAt(fenc) - '0';
                    break;

                case '/':
                case ' ':
                    rank--;
                    file = 0;
                    fenc++;
                    continue;
                default:
                    System.out.println("FEN error!");
                    return;
            }

            /*
             * By default the count is one, but if it's a space it might be more.
             * Add the number of pieces equal to the count.
             */
            for (int i = 0; i < count; i++) {
                int sq = (21 + file) + (10 * rank);
                board[sq] = piece;
                file++;
            }
            fenc++;
        }

        /*
         * Get the side to move
         */
        side = (fen.charAt(fenc) == 'w') ? WHITE : BLACK;
        fenc += 2;

        /*
         * Get the castling permissions
         */
        for (int i = 0; i < 4; i++) {
            if (fen.charAt(fenc) == ' ') {
                break;
            }
            switch (fen.charAt(fenc)) {
                case 'K':
                    castlePerm |= WKCA;
                    break;
                case 'Q':
                    castlePerm |= WQCA;
                    break;
                case 'k':
                    castlePerm |= BKCA;
                    break;
                case 'q':
                    castlePerm |= BQCA;
                    break;
                default:
                    break;
            }
            fenc++;
        }
        fenc++;

        /*
         * Get the en passant square
         */
        if (fen.charAt(fenc) != '-') {
            file = fen.charAt(fenc) - 'a';
            rank = fen.charAt(++fenc) - '1';
            enPas = (byte) ((21 + file) + (10 * rank));
        }

        /*
         * Set the position key and update the piece lists
         */
        posKey = generatePosKey();
        updatePieceListMaterial();
    }

    /**
     * Checks if a square is attacked by a side
     * @param sq the square number base 120
     * @param side the side it's attacked by
     * @return boolean if the square is attacked
     */
    private boolean isAttacked(int sq, int side) {
        /*
         * Pawn captures must be checked separately
         */
        if (side == WHITE) {
            if (board[sq - 11] == wP || board[sq - 9] == wP) return true;
        } else {
            if (board[sq + 11] == bP || board[sq + 9] == bP) return true;
        }

        /*
         * For each of the knight's direction check if the piece there is a knight of the opposite color
         */
        for (int i = 0; i < dirKn.length; i++) {
            byte pce = board[sq + dirKn[i]];
            if (pce != OFFBOARD && colPieces[pce] == side && pieceKnight[pce]) return true;
        }

        /*
         * For each of the bishop's directions check until it hits a piece or the end of the board,
         * then if it's a bishop or queen of the opposite color.
         */
        for (int i = 0; i < dirBi.length; i++) {
            int tsq = sq + dirBi[i];
            byte pce = board[tsq];
            while (pce != OFFBOARD) {
                if (pce != EMPTY) {
                    if (pieceBishopQueen[pce] && colPieces[pce] == side) return true;
                    break;
                }
                tsq += dirBi[i];
                pce = board[tsq];
            }
        }

        /*
         * For each of the rook's directions check until it hits a piece or the end of the board,
         * then if it's a rook or queen of the opposite color
         */
        for (int i = 0; i < dirRk.length; i++) {
            int tsq = sq + dirRk[i];
            byte pce = board[tsq];
            while (pce != OFFBOARD) {
                if (pce != EMPTY) {
                    if (pieceRookQueen[pce] && colPieces[pce] == side) return true;
                    break;
                }
                tsq += dirRk[i];
                pce = board[tsq];
            }
        }

        /*
         * For each of the king's directions check if the piece there is a king of the opposite color
         */
        for (int i = 0; i < dirKi.length; i++) {
            byte pce = board[sq + dirKi[i]];
            if (pce != OFFBOARD && colPieces[pce] == side && pieceKing[pce]) return true;
        }

        return false;
    }

    /**
     * Get the from square 120 out of a move integer
     * @param move move of integer format
     * @return byte square
     */
    private byte getFromSq(int move) {
        return getSquare120[move & 0x3F];
    }

    /**
     * Get the to square 120 out of a move integer
     * @param move move of integer format
     * @return byte square
     */
    private byte getToSq(int move) {
        return getSquare120[(move >> 6) & 0x3F];
    }

    /**
     * Get the captured piece out of a move integer
     * @param move move of integer format
     * @return byte piece number
     */
    private byte getCapturedPiece(int move) {
        return (byte) ((move >> 12) & 0xF);
    }

    /**
     * Get the promoted piece out of a move integer
     * @param move move of integer format
     * @return byte piece number
     */
    private byte getPromotedPiece(int move) {
        return (byte) ((move >> 16) & 0xF);
    }

    /**
     * Create a move integer when given the from square, to square, captured piece, promoted piece, and a special move flag
     * @param from the from square
     * @param to the to square
     * @param captured_piece any captured pieces
     * @param promoted_piece any promoted pieces
     * @param move_flag any special move flags
     * @return move of integer format
     */
    private int getMove(int from, int to, int captured_piece, int promoted_piece, int move_flag) {
        return getSquare64[from] | (getSquare64[to] << 6) | (captured_piece << 12) | (promoted_piece << 16) | move_flag;
    }

    /**
     * Add a capture move to the move list
     * @param move move of integer format
     */
    private void addCaptureMove(int move) {
        /*
         * Capture moves are scored by the MvvLva list when added to the list.
         * In addition all capture moves get + 1 000 000 score in the list, to make sure they are searched
         * before other moves and before killer moves
         */
        moveList[moveListStart[comPly + 1]] = move;
        moveScores[moveListStart[comPly + 1]++] = MvvLvaScore[getCapturedPiece(move) * 13 + board[getFromSq(move)]] + 1000000;
    }

    /**
     * Add a quiet move to the list
     * @param move move of integer format
     */
    private void addQuietMove(int move) {
        moveList[moveListStart[comPly + 1]] = move;

        /*
         * If the move is a killer move, add a bonus to the move ordering score
         * Otherwise set the move ordering score to the history score value of this move
         */
        if (move == searchKillers[comPly]) {
            moveScores[moveListStart[comPly + 1]++] = 900000;
        } else if (move == searchKillers[MAX_DEPTH + comPly]) {
            moveScores[moveListStart[comPly + 1]++] = 800000;
        } else {
            moveScores[moveListStart[comPly + 1]++] = searchHistory[board[getFromSq(move)] * BOARD_SQUARES + getToSq(move)];
        }
    }

    /**
     * Add an en passant move
     * @param move move of integer format
     */
    private void addEnPassantMove(int move) {
        /*
         * The en passant move is always a capture of pawn takes pawn, so the MvvLva score is always 105 + the 1000000 base
         */
        moveList[moveListStart[comPly + 1]] = move;
        moveScores[moveListStart[comPly + 1]++] = 1000105;
    }

    /**
     * Generate all possible capture moves
     */
    private void generateCaptureMoves() {
        moveListStart[comPly + 1] = moveListStart[comPly]; //Update the move list
        byte piece;

        /*
         * Check the side dependent pawn captures first
         */
        if (side == WHITE) {
            piece = wP;
            for (int i = 0; i < numPieces[piece]; i++) { //Loop through all of the squares with this piece
                byte sq = pieceListSquare[piece * 10 + i];
                /*
                 * Check if there is a black on the diagonal tiles
                 * If the pawn is on the 7th rank add promotion moves
                 */
                if (board[sq + 9] != OFFBOARD && colPieces[board[sq + 9]] == BLACK) {
                    if (getRank[sq] == 6) {
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wN, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wB, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wR, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], EMPTY, 0));
                    }
                }
                if (board[sq + 11] != OFFBOARD && colPieces[board[sq + 11]] == BLACK) {
                    if (getRank[sq] == 6) {
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wN, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wB, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wR, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], EMPTY, 0));
                    }
                }

                /*
                 * Check if the en passant square is available for capture
                 */
                if (enPas != NO_SQ) {
                    if (enPas == sq + 9) {
                        addEnPassantMove(getMove(sq, sq + 9, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    } else if (enPas == sq + 11) {
                        addEnPassantMove(getMove(sq, sq + 11, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    }
                }
            }

            /*
             * Set the piece to a white knight
             */
            piece = wN;
        } else {
            piece = bP;

            /*
             * Same for black
             */

            for (int i = 0; i < numPieces[piece]; i++) {
                byte sq = pieceListSquare[piece * 10 + i];
                if (board[sq - 9] != OFFBOARD && colPieces[board[sq - 9]] == WHITE) {
                    if (getRank[sq] == 2) {
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bN, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bB, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bR, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], EMPTY, 0));
                    }
                }
                if (board[sq - 11] != OFFBOARD && colPieces[board[sq - 11]] == WHITE) {
                    if (getRank[sq] == 2) {
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bN, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bB, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bR, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], EMPTY, 0));
                    }
                }
                if (enPas != NO_SQ) {
                    if (enPas == sq - 9) {
                        addEnPassantMove(getMove(sq, sq - 9, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    } else if (enPas == sq - 11) {
                        addEnPassantMove(getMove(sq, sq - 11, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    }
                }
            }

            /*
             * Set the piece to a black knight
             */
            piece = bN;
        }

        /*
         * Now the piece is equal to a knight of the side to move
         * Loop through all of the pieces until you pass the king of the color.
         * The king's piece number is 6 or 12, so the next piece is 7 or 13.
         * While the remainder is not 1 (i. e. the piece number is not 7 or 13)
         * Increment and loop through the pieces.
         * This will loop through all the non pawn pieces of the color to move.
         */

        while (piece % 6 != 1) {
            /*
             * Loop through all the squares for this type
             */
            for (int i = 0; i < numPieces[piece]; i++) {
                int sq = pieceListSquare[piece * 10 + i];

                /*
                 * Loop through all the directions for this piece
                 */
                for (int d = 0; d < pieceDir[piece].length; d++) {
                    int dir = pieceDir[piece][d];
                    int t_sq = sq + dir;

                    /*
                     * If it's a sliding piece continue until it hits either a piece or the end of the board.
                     * If it's a piece of the opposite color it can be captured.
                     */
                    if (slidePiece[piece]) {
                        while (board[t_sq] != OFFBOARD) {
                            if (board[t_sq] != EMPTY) {
                                if (colPieces[board[t_sq]] != side) {
                                    addCaptureMove(getMove(sq, t_sq, board[t_sq], EMPTY, 0));
                                }
                                break;
                            }
                            t_sq += dir;
                        }
                    } else {
                        if (board[t_sq] != OFFBOARD) {
                            if (board[t_sq] != EMPTY && colPieces[board[t_sq]] != side) {
                                addCaptureMove(getMove(sq, t_sq, board[t_sq], EMPTY, 0));
                            }
                        }
                    }
                }
            }
            piece++;
        }
    }

    /**
     * Generate all possible moves
     */
    private void generateMoves() {
        moveListStart[comPly + 1] = moveListStart[comPly]; // Update the move list
        byte piece;

        if (side == WHITE) {
            piece = wP;

            for (int i = 0; i < numPieces[piece]; i++) {
                byte sq = pieceListSquare[piece * 10 + i];
                /*
                 * If the square in front of a pawn is empty, add a pawn move
                 */
                if (board[sq + 10] == EMPTY) {
                    if (getRank[sq] == 1 && board[sq + 20] == EMPTY) {
                        /*
                         * If the pawn is on the 2nd rank, add a pawn move,
                         * and a pawn start move if the square 2 tiles in front is empty
                         */
                        addQuietMove(getMove(sq, sq + 10, EMPTY, EMPTY, 0));
                        addQuietMove(getMove(sq, sq + 20, EMPTY, EMPTY, MFLAG_PAWNSTART));
                    } else if (getRank[sq] == 6) {
                        /*
                         * If the pawn is on the 7th rank add promotion moves
                         */
                        addQuietMove(getMove(sq, sq + 10, EMPTY, wN, 0));
                        addQuietMove(getMove(sq, sq + 10, EMPTY, wB, 0));
                        addQuietMove(getMove(sq, sq + 10, EMPTY, wR, 0));
                        addQuietMove(getMove(sq, sq + 10, EMPTY, wQ, 0));
                    } else {
                        addQuietMove(getMove(sq, sq + 10, EMPTY, EMPTY, 0));
                    }
                }
                /*
                 * Same as for capture moves
                 */
                if (board[sq + 9] != OFFBOARD && colPieces[board[sq + 9]] == BLACK) {
                    if (getRank[sq] == 6) {
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wN, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wB, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wR, 0));
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], wQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq + 9, board[sq + 9], EMPTY, 0));
                    }
                }
                if (board[sq + 11] != OFFBOARD && colPieces[board[sq + 11]] == BLACK) {
                    if (getRank[sq] == 6) {
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wN, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wB, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wR, 0));
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], wQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq + 11, board[sq + 11], EMPTY, 0));
                    }
                }
                if (enPas != NO_SQ) {
                    if (enPas == sq + 9) {
                        addEnPassantMove(getMove(sq, sq + 9, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    } else if (enPas == sq + 11) {
                        addEnPassantMove(getMove(sq, sq + 11, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    }
                }
            }

            /*
             * If white can castle king side and the space in between is empty, add a castle move
             */
            if ((castlePerm & WKCA) != 0) {
                if (board[F1] == EMPTY && board[G1] == EMPTY) {
                    if (!isAttacked(F1, BLACK) && !isAttacked(E1, BLACK)) {
                        /*
                         * White can castle king side
                         */
                        addQuietMove(getMove(E1, G1, EMPTY, EMPTY, MFLAG_CASTLE));
                    }
                }
            }

            /*
             * If white can castle queen side and the space in between is empty, add a castle move
             */
            if ((castlePerm & WQCA) != 0) {
                if (board[B1] == EMPTY && board[C1] == EMPTY && board[D1] == EMPTY) {
                    if (!isAttacked(D1, BLACK) && !isAttacked(E1, BLACK)) {
                        /*
                         * White can castle queen side
                         */
                        addQuietMove(getMove(E1, C1, EMPTY, EMPTY, MFLAG_CASTLE));
                    }
                }
            }

            piece = wN;
        } else {
            /*
             * Same for black
             */
            piece = bP;

            for (int i = 0; i < numPieces[piece]; i++) {
                byte sq = pieceListSquare[piece * 10 + i];
                if (board[sq - 10] == EMPTY) {
                    if (getRank[sq] == 6 && board[sq - 20] == EMPTY) {
                        addQuietMove(getMove(sq, sq - 20, EMPTY, EMPTY, MFLAG_PAWNSTART));
                        addQuietMove(getMove(sq, sq - 10, EMPTY, EMPTY, 0));
                    } else if (getRank[sq] == 1) {
                        addQuietMove(getMove(sq, sq - 10, EMPTY, bN, 0));
                        addQuietMove(getMove(sq, sq - 10, EMPTY, bB, 0));
                        addQuietMove(getMove(sq, sq - 10, EMPTY, bR, 0));
                        addQuietMove(getMove(sq, sq - 10, EMPTY, bQ, 0));
                    } else {
                        addQuietMove(getMove(sq, sq - 10, EMPTY, EMPTY, 0));
                    }
                }
                if (board[sq - 9] != OFFBOARD && colPieces[board[sq - 9]] == WHITE) {
                    if (getRank[sq] == 1) {
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bN, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bB, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bR, 0));
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], bQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq - 9, board[sq - 9], EMPTY, 0));
                    }
                }
                if (board[sq - 11] != OFFBOARD && colPieces[board[sq - 11]] == WHITE) {
                    if (getRank[sq] == 1) {
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bN, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bB, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bR, 0));
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], bQ, 0));
                    } else {
                        addCaptureMove(getMove(sq, sq - 11, board[sq - 11], EMPTY, 0));
                    }
                }
                if (enPas != NO_SQ) {
                    if (enPas == sq - 9) {
                        addEnPassantMove(getMove(sq, sq - 9, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    } else if (enPas == sq - 11) {
                        addEnPassantMove(getMove(sq, sq - 11, EMPTY, EMPTY, MFLAG_ENPASSANT));
                    }
                }
            }

            if ((castlePerm & BKCA) != 0) {
                if (board[F8] == EMPTY && board[G8] == EMPTY) {
                    if (!isAttacked(F8, WHITE) && !isAttacked(E8, WHITE)) {
                        /*
                         * Black can castle king side
                         */
                        addQuietMove(getMove(E8, G8, EMPTY, EMPTY, MFLAG_CASTLE));
                    }
                }
            }
            if ((castlePerm & BQCA) != 0) {
                if (board[B8] == EMPTY && board[C8] == EMPTY && board[D8] == EMPTY) {
                    if (!isAttacked(D8, WHITE) && !isAttacked(E8, WHITE)) {
                        /*
                         * Black can castle queen side
                         */
                        addQuietMove(getMove(E8, C8, EMPTY, EMPTY, MFLAG_CASTLE));
                    }
                }
            }

            piece = bN;
        }

        /*
         * Same loop as for capture moves
         */

        while (piece % 6 != 1) {
            /*
             * Loop through all the squares for this type
             */
            for (int i = 0; i < numPieces[piece]; i++) {
                int sq = pieceListSquare[piece * 10 + i];

                /*
                 * Loop through all the directions for this piece
                 */
                for (int d = 0; d < pieceDir[piece].length; d++) {
                    int dir = pieceDir[piece][d];
                    int t_sq = sq + dir;

                    /*
                     * If it's a sliding piece continue until it hits either a piece or the end of the board.
                     * If it's an empty square it can move there, and if there is a piece of the opposite color there
                     * it can be captured.
                     */
                    if (slidePiece[piece]) {
                        while (board[t_sq] != OFFBOARD) {
                            if (board[t_sq] == EMPTY) {
                                addQuietMove(getMove(sq, t_sq, EMPTY, EMPTY, 0));
                            } else {
                                if (colPieces[board[t_sq]] != side) {
                                    addCaptureMove(getMove(sq, t_sq, board[t_sq], EMPTY, 0));
                                }
                                break;
                            }
                            t_sq += dir;
                        }
                    } else {
                        if (board[t_sq] != OFFBOARD) {
                            if (board[t_sq] == EMPTY) {
                                addQuietMove(getMove(sq, t_sq, EMPTY, EMPTY, 0));
                            } else if (colPieces[board[t_sq]] != side) {
                                addCaptureMove(getMove(sq, t_sq, board[t_sq], EMPTY, 0));
                            }
                        }
                    }
                }
            }
            piece++;
        }
    }

    /**
     * Check if the position has occurred earlier
     * @return boolean if it is a repetition
     */
    private boolean isRepetition() {
        /*
         * Loop through all the positions down to the last time the fifty move count was reset,
         * because if a pawn moves or a piece is captured the position can not occur again
         */
        for (int i = numPly - fiftyMove; i < numPly - 1; i++) {
            if (posKey == historyPosKey[i]) return true;
        }
        return false;
    }

    /**
     * Order the move list so that the next move in the list is the one with the highest score
     * @param moveNum the move list index start position
     */
    private void pickNextMove(int moveNum) {
        int bestscore = -INF;
        int bestNum = moveNum;

        /*
         * Loop through all the moves after this one in the move list, and find the one with the best score
         */
        for (int i = moveNum; i < moveListStart[comPly + 1]; i++) {
            if (moveScores[i] > bestscore) {
                bestscore = moveScores[i];
                bestNum = i;
            }
        }

        /*
         * Swap the move on top of the list with the best one, so that the next move search will be the best one
         */
        if (bestNum != moveNum) {
            int tmp = moveScores[moveNum];
            moveScores[moveNum] = moveScores[bestNum];
            moveScores[bestNum] = tmp;
            tmp = moveList[moveNum];
            moveList[moveNum] = moveList[bestNum];
            moveList[bestNum] = tmp;
        }
    }

    /**
     * Recursive method for calculating the best score after a given depth using alpha beta search
     * @param alpha the current max for the maximizer
     * @param beta the current min for the minimizer
     * @param depth the current depth
     * @return the best score
     */
    private int alphaBeta(int alpha, int beta, int depth) {
        searchnodes++;

        /*
         * If the search is over return the position evaluation
         */
        if (!searching) return evaluatePosition();

        /*
         * If the depth is zero, calculate capture moves to avoid the horizon effect
         */
        if (depth <= 0) {
            return quiescence(alpha, beta);
        }

        /*
         * If it's a repetition or more than fifty moves, the score is a draw
         */
        if ((isRepetition() || fiftyMove >= 100) && comPly != 0) return 0;

        /*
         * If the minimizer's lowest is better than a mate, return it
         */
        if (beta <= -MATE_SCORE) {
            return beta;
        }

        /*
         * If we have reached the max depth, return the position evaluation
         */
        if (comPly > MAX_DEPTH - 1) {
            return evaluatePosition();
        }

        /*
         * If the king is in check, increase the depth, to avoid missing forcing lines that lead to mate
         */
        boolean inCheck = isAttacked(pieceListSquare[sidesKings[side] * 10], side ^ 1);
        if (inCheck) {
            depth++;
        }

        int score;
        generateMoves();
        int legalmove = 0;
        int bestMove = 0;
        int alphaold = alpha;

        /*
         * Start searching the primary variation first, because the primary variation of previous depths
         * is most likely to be good the next depth as well
         * Search through all the moves and see if it's the primary variation
         */
        int pvMove = getPVMove();
        if (pvMove != 0) {
            for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
                if (moveList[i] == pvMove) {
                    moveScores[i] = 2000000;
                    break;
                }
            }
        }

        /*
         * Loop through all of the moves, and pick the one with the highest score
         */
        for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
            pickNextMove(i);
            int move = moveList[i];
            if (makeMove(move)) {
                /*
                 * If the move was legal, set the score to the opposite alpha beta search for the next depth,
                 * because every other move the players want to min and max their results.
                 * Revert the move afterwards.
                 */
                legalmove++;
                score = -alphaBeta(-beta, -alpha, depth - 1);
                revertMove();

                /*
                 * If the score is better than the current best, update the current best, and check if we have a beta
                 * cut off. If the move is a non capture one, it's classified as a killer move, and added to the killer
                 * move list. Otherwise, the add the score in the form of the depth * depth to the move score.
                 * This means that if a move is good in many positions, it will most likely be good in this position as
                 * well. Return the beta score if we had a beta cut off, because that is the best possible score.
                 */
                if (score > alpha) {
                    if (score >= beta) {
                        if (legalmove == 1) {
                            searchbcf++;
                        }
                        searchbc++;
                        if ((move & MFLAG_CAPTURE) == 0) {
                            searchKillers[MAX_DEPTH + comPly] = searchKillers[comPly];
                            searchKillers[comPly] = move;
                        }
                        return beta;
                    }
                    if ((move & MFLAG_CAPTURE) == 0) {
                        searchHistory[board[getFromSq(move)] * BOARD_SQUARES + getToSq(move)] += depth * depth;
                    }
                    alpha = score;
                    bestMove = move;
                }
            }
        }

        /*
         * If there was no legal moves, return mate score if the king is in check, otherwise return 0 for a draw
         */
        if (legalmove == 0) {
            if (inCheck) {
                return -MATE_SCORE + comPly;
            } else {
                return 0;
            }
        }

        /*
         * If the alpha was improved, store this move in the primary variation
         */
        if (alpha != alphaold) {
            storePVMove(bestMove);
        }

        /*
         * Return the best possible score
         */
        return alpha;
    }

    /**
     * Recursively calculate the quiescence score the same way as alpha beta, except calculate only capture moves.
     * This is to avoid the horizon effect
     * @param alpha the maximizers best score
     * @param beta the minimizers best score
     * @return the best possible score
     */
    private int quiescence(int alpha, int beta) {
        /*
         * This method works exactly like alpha beta
         */
        searchnodes++;

        if (!searching) return evaluatePosition();

        if ((isRepetition() || fiftyMove >= 100) && comPly != 0) return 0;

        if (comPly > MAX_DEPTH - 1) {
            return evaluatePosition();
        }

        int score = evaluatePosition();

        if (score >= beta) {
            return beta;
        }

        if (score > alpha) {
            alpha = score;
        }

        generateCaptureMoves();
        int legalmove = 0;
        int bestMove = 0;
        int alphaold = alpha;

        for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
            pickNextMove(i);
            int move = moveList[i];
            if (makeMove(move)) {
                legalmove++;
                score = -quiescence(-beta, -alpha);
                revertMove();
                if (score > alpha) {
                    if (score >= beta) {
                        if (legalmove == 1) {
                            searchbcf++;
                        }
                        searchbc++;
                        return beta;
                    }
                    alpha = score;
                    bestMove = move;
                }
            }
        }

        if (alpha != alphaold) {
            storePVMove(bestMove);
        }

        return alpha;
    }

    /**
     * Clear the currently stored primary variation
     */
    private void clearPrimaryVariation() {
        /*
         * Loop through all the primary variation entries and set them to 0
         */
        for (int i = 0; i < PV_ENTRIES; i++) {
            primaryVariationMove[i] = 0;
            primaryVariationPositionKey[i] = 0;
        }
    }

    /**
     * Clear the board variables for search
     */
    private void clearForSearch() {
        /*
         * Clear the search history and the killer moves
         */
        for (int i = 0; i < 13 * BOARD_SQUARES; i++) {
            searchHistory[i] = 0;
        }
        for (int i = 0; i < 2 * MAX_DEPTH; i++) {
            searchKillers[i] = 0;
        }
        clearPrimaryVariation();
        comPly = 0;
        searchbc = 0;
        searchbcf = 0;
        searchnodes = 0;
    }

    /**
     * Search the current position from the start depth to the end depth
     * @param start the start depth
     * @param depth the end depth
     * @return the best move of integer format
     */
    private int searchPosition(int start, int depth) {
        /*
         * While searching disable the rendering and player input
         */
        searching = true;
        playerMove = false;

        /*
         * Input validation
         */
        if (start > depth) start = depth;
        if (start < 1) start = 1;
        searchdepth = depth;
        int bestMove = 0;
        int bestScore;
        int currentDepth;

        clearForSearch();

        /*
         * Loop through the different depths and search them
         */
        for (currentDepth = start; currentDepth <= searchdepth; currentDepth++) {
            if (!searching) break; // Stop searching

            /*
             * Get the best possible score and print the primary variation to the screen
             */
            bestScore = alphaBeta(-INF, INF, currentDepth);
            bestMove = getPVMove();
            String line = "D" + currentDepth;

            /*
             * If the score is a mate score, show the number of moves until mate
             */
            if (Math.abs(bestScore) >= MATE_SCORE - MAX_DEPTH) {
                line += " Mate in " + (MATE_SCORE - Math.abs(bestScore)) + " nodes: " + searchnodes;
            } else {
                line += " Score: " + bestScore + " nodes: " + searchnodes;
            }
            double rat = searchbcf * 1.0 / searchbc;
            int pct = (int) Math.round(rat * 100.0);
            line += " Ordering: " + pct + "%";
            line += " Best: ";
            int n = getPvLine(currentDepth);
            for (int c = 0; c < n; c++) {
                line += " " + moveToString(primaryVariation[c]);
            }
            frame.setTitle((side == BLACK ? "White" : side == WHITE ? "Black" : "None") + " to move. Score: " + (side == BLACK ? -bestScore : bestScore)); // Update the title
            System.out.println(line);
        }

        /*
         * Allow for rendering and player moves
         */
        searching = false;
        playerMove = true;
        return bestMove;
    }

    /**
     * Simple heuristic function for evaluating the position
     *
     * @return the evaluation score
     */
    private int evaluatePosition() {
        /*
         * Scores for black are subtracted and scores for white are added
         * Start by adding together the material scores
         */
        int score = material[WHITE] - material[BLACK];

        /*
         * Loop through all the pieces and add the value they get when they are on a spesific square
         */
        byte piece = wP;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score += pawnScoreTable[getSquare64[sq]];
        }

        piece = bP;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score -= pawnScoreTable[mirror64[getSquare64[sq]]];
        }

        piece = wN;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score += knightScoreTable[getSquare64[sq]];
        }

        piece = bN;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score -= knightScoreTable[mirror64[getSquare64[sq]]];
        }

        piece = wB;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score += bishopScoreTable[getSquare64[sq]];
        }

        piece = bB;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score -= bishopScoreTable[mirror64[getSquare64[sq]]];
        }

        piece = wR;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score += rookScoreTable[getSquare64[sq]];
        }

        piece = bR;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score -= rookScoreTable[mirror64[getSquare64[sq]]];
        }

        piece = wQ;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score += queenScoreTable[getSquare64[sq]];
        }

        piece = bQ;
        for (int i = 0; i < numPieces[piece]; i++) {
            byte sq = pieceListSquare[piece * 10 + i];
            score -= queenScoreTable[mirror64[getSquare64[sq]]];
        }

        /*
         * Add an extra bonus for the bishop pair
         */
        if (numPieces[wB] > 1) {
            score += scoreBishopPair;
        }
        if (numPieces[bB] > 1) {
            score -= scoreBishopPair;
        }

        return side == WHITE ? score : -score;
    }

    /**
     * Get the primary variation in this position
     *
     * @return move of integer format
     */
    private int getPVMove() {
        int index = (int) (posKey % PV_ENTRIES);

        /*
         * If the primary variation written to this index has the same position key as this one,
         * we know it is written from this key
         */
        if (primaryVariationPositionKey[index] == posKey) {
            return primaryVariationMove[index];
        }

        return 0;
    }

    /**
     * Store the move in the primary variation line
     *
     * @param move the move to store
     */
    private void storePVMove(int move) {
        /*
         * Set both the move, and the original posKey on the index
         */
        int index = (int) (posKey % PV_ENTRIES);
        primaryVariationMove[index] = move;
        primaryVariationPositionKey[index] = posKey;
    }

    /**
     * Set up the primary variation line so that we can read it from the primaary variation array
     *
     * @param depth the depth to search for a variation up to
     * @return the number of moves found in the primary variation
     */
    private int getPvLine(int depth) {
        int move = getPVMove();
        int count = 0;
        while (move != 0 && count < depth) {
            /*
             * Loop through all the moves registered in the primary variation, and attempt it,
             * Then update the primary variation and get the next move
             */
            if (moveExists(move)) {
                makeMove(move);
                primaryVariation[count++] = move;
                move = getPVMove();
            } else {
                break;
            }
        }

        /*
         * Afterwards revert the number of moves made int the variation
         */
        while (comPly > 0) {
            revertMove();
        }

        return count;
    }

    /**
     * Check if a move exists
     * @param move move of integer format to check existence of
     * @return boolean representing the existence
     */
    private boolean moveExists(int move) {
        /*
         * Generate the moves and check if the move exists in the list, and the move is legal
         */
        generateMoves();
        for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
            int foundMove = moveList[i];
            if (!makeMove(foundMove)) {
                continue;
            }
            revertMove();
            if (move == foundMove) return true;
        }
        return false;
    }

    /**
     * Attempt to execute a move from an input string
     * @param move move string in the format a1h8p where the 4 first chars represent the from and to square, and the 5th char represents a promotion piece
     * @return boolean if the execution succeeded
     */
    private boolean attemptMove(String move) {
        /*
         * Get the ranks and files from the string
         */
        int from_file = move.charAt(0) - 'a';
        int from_rank = move.charAt(1) - '1';
        int to_file = move.charAt(2) - 'a';
        int to_rank = move.charAt(3) - '1';

        /*
         * Check if the squares are on the board
         */
        if (from_file > 7 || to_file > 7 || from_rank > 7 || to_rank > 7 || from_file < 0 || to_file < 0 || from_rank < 0 || to_rank < 0)
            return false;

        /*
         * Get the from and to squares base 120
         */
        byte from_sq = getSquare120[from_file + from_rank * 8];
        byte to_sq = getSquare120[to_file + to_rank * 8];

        byte piece = board[from_sq];

        /*
         * The target piece must be of the same side as the move
         */
        if (colPieces[piece] != side) return false;

        /*
         * Check if the from and to squares match up with a move in the move list
         */
        generateMoves();
        for (int i = moveListStart[comPly]; i < moveListStart[comPly + 1]; i++) {
            int lmove = moveList[i];
            if (getFromSq(lmove) == from_sq && getToSq(lmove) == to_sq) {
                if ((lmove & MFLAG_PROMOTION) != 0) {
                    /*
                     * If the move is a promotion piece, check the last char for the promotion piece
                     * by looping through the piece chars, and finding the piece of the right color.
                     */
                    for (byte c = 0; c < pieceChars.length; c++) {
                        if (pieceChars[c] == move.charAt(4)) {
                            int pce = c;
                            if (colPieces[c] != side) {
                                if (side == WHITE) pce = c - 6;
                                else pce = c + 6;
                            }
                            /*
                             * Create a new move with the promotion piece and return the move status,
                             * if it is legal or not.
                             */
                            int newMove = getMove(getFromSq(lmove), getToSq(lmove), getCapturedPiece(lmove), pce, 0);
                            return makeMove(newMove);
                        }
                    }

                    /*
                     * This means the promotion char was not a valid piece char
                     */
                    return false;
                }

                /*
                 * Make the move, and return the move status, if it is legal or not
                 */
                return makeMove(lmove);
            }
        }

        /*
         * This means the move was not in the move list
         */
        return false;
    }

    /**
     * Inner class for processing threads that are controlled by the control variable running
     */
    private static class ProcessThread extends Thread {
        public boolean running;
    }
}
