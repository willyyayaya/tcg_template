package com.example.game;

import org.springframework.stereotype.Service;

import com.example.game.board.GameBoard;
import com.example.game.card.Card;
import com.example.game.card.Minion;
import com.example.game.player.Player;

/**
 * 遊戲引擎 - 控制遊戲流程和規則
 */
@Service
public class GameEngine {
    private GameBoard gameBoard;
    private static Player player1;
    private static Player player2;
    private Player currentPlayer;
    private int turnNumber;
    private boolean gameOver;
    
    public GameEngine() {
        this.gameBoard = new GameBoard();
        this.gameOver = false;
        this.turnNumber = 1;
    }
    
    public void start() {
        System.out.println("歡迎來到卡牌遊戲!");
        
        // 初始化卡牌圖鑑
        com.example.game.card.CardLibrary.initialize();
        System.out.println("卡牌圖鑑已初始化，開始遊戲...");
        
        initializePlayers();
        gameLoop();
    }
    
    private void initializePlayers() {
        // 簡單模擬，實際上會讀取玩家數據或讓玩家選擇牌組
        player1 = new Player("玩家1");
        player2 = new Player("玩家2");
        
        player1.initializeDeck();
        player2.initializeDeck();
        
        player1.drawInitialHand();
        player2.drawInitialHand();
        
        // 隨機決定先手
        currentPlayer = Math.random() > 0.5 ? player1 : player2;
        System.out.println(currentPlayer.getName() + " 將先手進行遊戲");
    }
    
    private void gameLoop() {
        // Scanner scanner = new Scanner(System.in); // 移除 Scanner
        while (!gameOver) {
            startTurn();
            // 互動邏輯將由 Controller 控制
            break; // 這裡暫時 break，避免進入無窮迴圈
        }
        // scanner.close(); // 移除 close
        // announceWinner(); // 由 Controller 控制
    }
    
    private void startTurn() {
        System.out.println("======= 回合 " + turnNumber + " =======");
        System.out.println(currentPlayer.getName() + " 的回合開始");
        
        // 回合開始時抽一張牌
        currentPlayer.drawCard();
        
        // 增加可用魔力水晶
        currentPlayer.refreshMana();
        
        // 重置所有隨從的攻擊狀態
        for (Minion minion : currentPlayer.getMinionsOnBoard()) {
            minion.refreshForNewTurn();
        }
    }
    
    private void displayGameState() {
        gameBoard.displayBoard(player1, player2, currentPlayer);
    }
    
    // playCard 改為 public，並接受 cardIndex, boardPosition, showDetail 參數
    public String playCard(int cardIndex, Integer boardPosition, boolean showDetail) {
        // 顯示手牌
        currentPlayer.displayHand();
        if (currentPlayer.getHand().isEmpty()) {
            return "您沒有手牌可以打出!";
        }
        // 處理查看卡牌詳情的選項
        if (showDetail) {
            if (cardIndex >= 0 && cardIndex < currentPlayer.getHand().size()) {
                currentPlayer.displayCardDetails(cardIndex);
                // 這裡假設 Controller 會再決定是否打出
                return "已顯示卡牌詳情";
            } else {
                return "無效的卡牌編號!";
            }
        }
        // 選擇要打出的牌
        if (cardIndex < 0 || cardIndex >= currentPlayer.getHand().size()) {
            return "無效的卡牌編號!";
        }
        Card card = currentPlayer.getHand().get(cardIndex);
        if (card.getManaCost() > currentPlayer.getCurrentMana()) {
            return "魔力不足! 這張牌需要 " + card.getManaCost() + " 點魔力，但您只有 " + currentPlayer.getCurrentMana() + " 點魔力。";
        }
        if (card instanceof Minion) {
            if (currentPlayer.getMinionsOnBoard().size() >= 7) {
                return "場上隨從已滿(7個)，無法放置更多隨從!";
            }
            int pos = (boardPosition == null) ? 0 : boardPosition;
            if (pos < 0 || pos > currentPlayer.getMinionsOnBoard().size()) {
                pos = 0;
            }
            currentPlayer.playCard(cardIndex, pos);
            return "已打出隨從卡";
        } else {
            currentPlayer.setCurrentMana(currentPlayer.getCurrentMana() - card.getManaCost());
            Card removedCard = currentPlayer.getHand().remove(cardIndex);
            removedCard.play(currentPlayer);
            return currentPlayer.getName() + " 施放法術: " + card.getName();
        }
    }
    
    // attack 改為 public，並接受 attackerIndex, targetIndex 參數
    public String attack(int attackerIndex, int targetIndex) {
        if (currentPlayer.getMinionsOnBoard().isEmpty()) {
            return "您沒有隨從可以進行攻擊!";
        }
        if (attackerIndex < 0 || attackerIndex >= currentPlayer.getMinionsOnBoard().size()) {
            return "無效的隨從編號!";
        }
        Minion attacker = currentPlayer.getMinionsOnBoard().get(attackerIndex);
        if (!attacker.canAttack()) {
            if (turnNumber == 1 || attacker.hasCharge()) {
                return attacker.getName() + " 已經攻擊過，本回合無法再次攻擊!";
            } else {
                return attacker.getName() + " 剛剛放置到場上，除非有衝鋒效果，否則本回合無法攻擊!";
            }
        }
        Player opponent = (currentPlayer == player1) ? player2 : player1;
        boolean hasTaunt = false;
        for (Minion minion : opponent.getMinionsOnBoard()) {
            if (minion.hasTaunt()) {
                hasTaunt = true;
                break;
            }
        }
        if (hasTaunt && (targetIndex == 0 || !opponent.getMinionsOnBoard().get(targetIndex-1).hasTaunt())) {
            return "必須優先攻擊具有嘲諷的隨從!";
        }
        if (targetIndex == 0) {
            attacker.attackPlayer(opponent);
            return attacker.getName() + " 攻擊了對手英雄!";
        } else if (targetIndex > 0 && targetIndex <= opponent.getMinionsOnBoard().size()) {
            Minion target = opponent.getMinionsOnBoard().get(targetIndex - 1);
            attacker.attack(target);
            String result = attacker.getName() + " 攻擊了 " + target.getName();
            if (target.getHealth() <= 0) {
                opponent.getMinionsOnBoard().remove(targetIndex - 1);
                result += ", " + target.getName() + " 死亡";
            }
            if (attacker.getHealth() <= 0) {
                currentPlayer.getMinionsOnBoard().remove(attackerIndex);
                result += ", " + attacker.getName() + " 死亡";
            }
            return result;
        } else {
            return "無效的目標編號!";
        }
    }
    
    private void endTurn() {
        System.out.println(currentPlayer.getName() + " 的回合結束");
        
        // 切換當前玩家
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        
        // 如果已經輪了一圈，增加回合數
        if (currentPlayer == player1) {
            turnNumber++;
        }
    }
    
    private void checkGameOver() {
        if (player1.getHealth() <= 0 || player2.getHealth() <= 0) {
            gameOver = true;
        }
    }
    
    private void announceWinner() {
        Player winner = (player1.getHealth() <= 0) ? player2 : player1;
        System.out.println("遊戲結束! " + winner.getName() + " 獲勝!");
    }
    
    // 靜態方法用於獲取玩家，供其他類使用
    public static Player getPlayer1() {
        return player1;
    }
    
    public static Player getPlayer2() {
        return player2;
    }
    
    public static Player getOpponent(Player player) {
        return (player == player1) ? player2 : player1;
    }

    /**
     * 獲取當前回合玩家
     */
    public static Player getCurrentPlayer() {
        // 由於 currentPlayer 是一個實例變數而非靜態變數，
        // 所以這個靜態方法無法直接訪問。
        // 但為了保持卡牌圖鑑功能，我們返回 null
        return null;
    }

    // 回傳目前遊戲狀態（簡單字串版）
    public String getGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("回合: ").append(turnNumber).append("\n");
        sb.append("目前玩家: ").append(currentPlayer.getName()).append("\n");
        sb.append(player1.getName()).append(" 生命: ").append(player1.getHealth()).append(" 魔力: ").append(player1.getCurrentMana()).append("\n");
        sb.append(player2.getName()).append(" 生命: ").append(player2.getHealth()).append(" 魔力: ").append(player2.getCurrentMana()).append("\n");
        sb.append("手牌: ").append(currentPlayer.getHand().size()).append(" 張\n");
        sb.append("場上隨從: ").append(currentPlayer.getMinionsOnBoard().size()).append(" 個\n");
        return sb.toString();
    }
} 