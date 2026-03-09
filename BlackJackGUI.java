package finalProject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class BlackJackGUI extends JFrame implements ActionListener 
{

    private static final long serialVersionUID = 1L;
    
    private JPanel gamePanel;
    // Visual Constants 
    Color table_color = new Color(53, 101, 77);
    int cardWidth = 110;
    int cardHeight = 154;
    int x = 190; // Horizontal offset to center the cards on screen
    private Color textColor = Color.WHITE;
    private JLabel deckLabel;
    
 	//themes variables
    private String currentBgPath = "/finalProject/cards/background.png"; 
    private String currentBackPath = "/finalProject/cards/BACK.png";     
    private JButton themeToggle;
    private boolean isXmasTheme = false; 
    
    // Game Logic Objects
    private Deck deck; //deck of cards
    private JComboBox<Integer> deckSelector; // Dropdown to select number of decks
    //declare the object but create it after asking the user for its name
    Player player;
    //new dealer object
    Dealer dealer = new Dealer();
    // Starting money
    private double balance = 10.00;
    //default bet 
    private double bet=1;
    
    private PayoutMessage activePayoutMessage = null;
    private final int PAYOUT_ANIMATION_SPEED = 50; // 50ms timer speed
    private JSpinner betSpinner; // bet field (spinner)
    
    private boolean roundOver = false;
    private String resultMessage = "";

    // variable to hold the hidden card reference (needed for flipping)
    private Card dealerHidden;
    
    // --- BUTTONS (BOTTOM) ---
    private JButton hitButton = new GoldButton("HIT");
    private JButton standButton = new GoldButton("STAND");
    private JButton betButton = new GoldButton("BET");
    private JButton restartButton = new GoldButton("RESTART");
    private JButton doubleButton = new GoldButton("DOUBLE");
    private boolean isDoubling = false;
    
    // animation
    private final Timer animationTimer;
    private final int ANIMATION_SPEED = 30; // 30ms per frame
    private Queue<AnimationCard> animationQueue = new LinkedList<>();
    private boolean isDealerDrawing = false; // state flag to manage the dealer's drawing phase
    
    //Inner Class: AnimationCard
    private class AnimationCard 
    {
        Card card;
        Player target;// destination can be a Player OR a Dealer
        Point end; // The final X,Y coordinates where the card should land
        Point start; // The starting X,Y coordinates (the deck position)
        Point current; // The current X,Y coordinates during the animation
        int totalSteps = 15; // number of steps for animation
        int stepsLeft;
        boolean isHoleCard;// Flag: Should this card be drawn face down?
        
        //constructor for animation
        public AnimationCard(Card c, Player t, Point e, boolean isHidden) {
            this.card = c;
            this.target = t;
            this.end = e;
            this.start = new Point(1100, 0); // starting point (top right)
            this.current = new Point(start.x, start.y);
            this.stepsLeft = totalSteps;
            this.isHoleCard = isHidden;
        }
        // Method called every frame to move the card
        public boolean step() 
        {
            if (stepsLeft <= 0) {
                // Ensure final snap to end
                current.setLocation(end);
                return true; // animation finished
            }

            // Use remaining steps so we don't get uneven small movements
            int dx = (end.x - current.x) / stepsLeft;
            int dy = (end.y - current.y) / stepsLeft;

            // If dx or dy becomes 0 because of integer division and not yet at end,
            // nudge at least one pixel so the animation completes.
            if (dx == 0 && current.x != end.x) dx = (end.x > current.x) ? 1 : -1;
            if (dy == 0 && current.y != end.y) dy = (end.y > current.y) ? 1 : -1;

            current.translate(dx, dy);
            stepsLeft--;

            if (stepsLeft == 0) {
                current.setLocation(end); // final snap to end point to avoid rounding errors
                return true;
            }

            return false; // animation not finished
        }
    }
    
    // New method to handle the dealer drawing animation loop
    private void dealerDrawLoop() 
    {
        isDealerDrawing = true;
        
        // Disable buttons while dealer draws
        hitButton.setEnabled(false);
        standButton.setEnabled(false);
        betButton.setEnabled(false);
        
        //If score is less than 17, Dealer MUST hit
        if (dealer.getScore() < 17) 
        {
            // Queue up ONE animated card deal
            dealCardAnimated(dealer, false); // Deal face up
        } else {
            // Dealer is done drawing (score >= 17)
            isDealerDrawing = false;
            
            // Perform final settlement and show message
            settleRound();
                       
            //if player lost everything, lock controls
            if (balance<=0) 
            {
            	hitButton.setEnabled(false);
                standButton.setEnabled(false);
                betButton.setEnabled(false);
                deckSelector.setEnabled(false);
            }

            if (balance > 0) 
            {
               
            }
        }
    }
    
    // check if Player busted or hit 21 during their turn
    private void checkPlayerState() 
    {
    	if (!animationQueue.isEmpty()) {
            return; // Esperar a que terminen todas las animaciones pendientes
        }
    	
        int p = player.getScore();
        if (p >= 21) 
        {
            // Player busts or hits 21, initiate the Dealer's turn (animated).
            // processStand() handles flipping the hidden card, the 500ms delay,
            // the animated dealer draw loop, and the final settlement.
            processStand(); 
        }
    }

    // animation flow
    private void handleAnimationTick(ActionEvent e) 
    {
    	if(gamePanel == null) {
            animationTimer.stop();
            return;
        }
    	// If there are no cards moving
    	if(animationQueue.isEmpty()) 
    	{
    		animationTimer.stop();
    		
    		// --- Logic when queue is empty ---
            if (!isDealerDrawing) 
            {
                // Initial Deal finished (or Player Hit finished)
            	if(!roundOver) 
                {

                    if (isDoubling) {
                        isDoubling = false; // Resetear bandera
                        // Si el jugador no se pasó de 21
                        // forzamos el Stand automáticamente.
                        if (player.getScore() <= 21) {
                            processStand();
                        }
                        return; // Salir para no reactivar botones
                    }
                    // -------------------------------------

                    // Re-enable buttons ONLY for gameplay
                    hitButton.setEnabled(true);
                    standButton.setEnabled(true);

                    // Solo se puede doblar si tienes 2 cartas Y saldo suficiente
                    if (player.getHandSize() == 2 && balance >= bet) {
                        doubleButton.setEnabled(true);
                    } else {
                        doubleButton.setEnabled(false);
                    }
                }
                
                maybeAutoSettleOnInitialDeal(); 
            } 
            
            else 
            {
                // Dealer Draw finished one card. Resume the draw loop to check if another card is needed.
                dealerDrawLoop();
            }
            
    		return;
    	}
    	
    	// If queue is not empty, get the next animation
	    AnimationCard currentAnim = animationQueue.peek();
	    
	    // step the card position
	    boolean finished = currentAnim.step();
	 // If the card arrived at its destination:
	    if(finished) 
	    {
	    	// animation is done: finalize card state
	    	Card c = currentAnim.card;
	    	
	    	// add card to the target's hand *now that it reached its destination*
	        currentAnim.target.addCard(c);
	    	
	    	if(currentAnim.isHoleCard) 
	    	{
	    		dealerHidden = c; // save reference to the hidden card
	    		c.flipDown();
	    	} 
	    	else {
	    		c.flipUp();
	    	}
	    	
	    	animationQueue.poll(); // Remove the finished animation from queue
	    	
	    	// If the card went to the player, check if they busted
	    	if (currentAnim.target == player) 
	    	{
	    	    checkPlayerState();
	    	}
	    	
	    	// If dealer was drawing and the animation finished, call the loop to check the next card
	        if (isDealerDrawing && animationQueue.isEmpty()) 
	        {
	            dealerDrawLoop();
	        }
	     	
	        // If there's another card already queued, start the timer immediately for smooth flow
	        if (!animationQueue.isEmpty()) 
	        {
	            animationTimer.start();
	        }
	    }
	    gamePanel.repaint(); // Redraw the screen to show the new card position
    }
    
    // Helper to create an animation and add it to queue
    private void dealCardAnimated(Player target, boolean isHidden) 
    {
    	// calculate card final position in the target's hand
    	Card c = deck.deal();
    	int cardsInQueue = 0;
        for(AnimationCard anim : animationQueue) {
            if(anim.target == target) {
                cardsInQueue++;
            }
        }
        
    	int cardCount = target.getHandSize()+ cardsInQueue;; // Hand size has already been incremented by the AnimationCard constructor
    	// Assuming cardWidth, x, 20, and 320 are defined constants/fields
        int xPos = 100 + cardCount * (cardWidth + 5) + x + 100;
        
    	Point endPoint;
    	//Is the target a Dealer?
    	if (target instanceof Dealer)  
    	{
            endPoint = new Point(xPos, 230); // dealer position (top)
        } 
    	else 
    	{
            endPoint = new Point(xPos, 450); // player position (bottom)
        }
    	
    	// create the AnimationCard
    	AnimationCard animCard = new AnimationCard(c, target, endPoint, isHidden);
    	
    	// add it to the queue
    	animationQueue.add(animCard);
    	
    	// start the timer if it's stopped and there's work to do
    	if(!animationTimer.isRunning()) {
    		animationTimer.start();
    	}
    }
   
    // SHOW WIN/LOSE MESSAGE
    private void showMessage(String text) 
    {
        JOptionPane.showMessageDialog(
            BlackJackGUI.this,           // parent window
            text,                        // message
            "Message",                   // title
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    // PAYOUT MESSAGE
    class PayoutMessage 
    {
        public String text;
        public Color color;
        public int x, y; // Current position
        public int alpha = 255; // Transparency (255=Opaque, 0=Invisible)

        public PayoutMessage(String text, Color color, int x, int y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }
    
    // MAY BE AUTOSETTLE
    private void maybeAutoSettleOnInitialDeal() 
    {
        // Ensure this only runs when the initial deal animation is truly finished
        if (!animationQueue.isEmpty()) {
        	return;
        }

        int p = player.getScore();
        if (p == 21) {
            // Reveal hidden card and START dealer loop to check for dealer's natural 21
            if (dealerHidden != null) {
            	dealerHidden.flipUp();
            }
            
            // Start dealer drawing, which will immediately settle if dealer score is >= 17
            isDealerDrawing = true;
            dealerDrawLoop(); 
        }
    }
    
    private void settleRound() 
    {
        int dealerScore = dealer.getScore();
        int playerScore = player.getScore();
        
        // El dinero YA fue descontado al pulsar BET.
        // Aquí calculamos cuánto devolver (Pago total: Apuesta + Ganancia).
        double payout = 0; 
        String displayAmount = "";
        Color msgColor = null;

        // Verificar si es Blackjack Natural (21 con 2 cartas)
        boolean isBlackjack = (playerScore == 21 && player.getHandSize() == 2);

        if (isBlackjack) {
            // Regla: Si el dealer también tiene 21 con 2 cartas, es EMPATE
            if (dealerScore == 21 && dealer.getHandSize() == 2) {
                payout = bet; // Te devuelve tu dinero
                displayAmount = "PUSH";
                msgColor = Color.GRAY;
            } 
            else 
            {
                // Ganas con Blackjack: Se paga 3 a 2 (Total x2.5)
                payout = bet * 2.5; 
                displayAmount = "BLACKJACK! +$" + String.format("%.2f", payout);
                msgColor = new Color(255, 215, 0); // Dorado
            }
        } 
        else if (playerScore > 21) 
        {
            // Pierdes (Bust)
            payout = 0;
            displayAmount = "YOU LOSE! -$" + String.format("%.2f", bet);
            msgColor = Color.RED;
        } 
        else if (dealerScore > 21) {
            // Dealer se pasa. Ganas 1 a 1 (Total x2.0)
            payout = bet * 2.0;
            displayAmount = "DEALER LOSE! +$" + String.format("%.2f", payout);
            msgColor = new Color(0, 200, 0); // Verde
        }
        else if (playerScore > dealerScore) {
            // Tienes más puntos. Ganas 1 a 1 (Total x2.0)
            payout = bet * 2.0;
            displayAmount = "YOU WIN! +$" + String.format("%.2f", payout);
            msgColor = new Color(0, 200, 0); 
        } 
        else if (playerScore < dealerScore) {
            // Dealer tiene más puntos. Pierdes
            payout = 0;
            displayAmount = "DEALER WINS! -$" + String.format("%.2f", bet);
            msgColor = Color.RED;
        } 
        else {
            // Empate (Push). Te devuelve tu dinero.
            payout = bet;
            displayAmount = "PUSH";
            msgColor = Color.GRAY;
        }
        
        balance += payout; // Actualizar saldo con el premio
        
        roundOver = true; 
        
        // Mensaje flotante
        activePayoutMessage = new PayoutMessage(displayAmount, msgColor, 620, 450); 
        startPayoutAnimation(); 
        
     // Si el jugador aún tiene dinero, le dejamos apostar de nuevo INMEDIATAMENTE
        if (balance > 0) {
            betButton.setEnabled(true);      // ACTIVAR
            betSpinner.setEnabled(true);     // ACTIVAR
            deckSelector.setEnabled(true);   // ACTIVAR
            
            hitButton.setEnabled(false);     // Desactivar acciones de juego
            standButton.setEnabled(false);
            doubleButton.setEnabled(false);
        } 
        else 
        {
            // Si se quedó en $0, entonces sí bloqueamos todo
            checkGameOver();
        }
    }
    


    private Timer payoutTimer;

    private void startPayoutAnimation() {
        if (payoutTimer != null && payoutTimer.isRunning()) {
            payoutTimer.stop();
        }
        
        payoutTimer = new Timer(PAYOUT_ANIMATION_SPEED, e -> {
            if (activePayoutMessage == null) {
                payoutTimer.stop();
                return;
            }

            // 1. Move the message up (Negative dy moves it up on the screen)
            activePayoutMessage.y -= 4; 
            
            // 2. Fade out (Decrease alpha)
            activePayoutMessage.alpha -= 7; 

            // 3. Check for completion
            if (activePayoutMessage.alpha <= 0) { // Stop if transparent
                activePayoutMessage = null;
                payoutTimer.stop();
                if(balance>0) {
                	//startNewRoundDelayed();
                }
            }

            gamePanel.repaint(); // Redraw the screen to show the new position/fade
        });
        payoutTimer.start();
    }
    
    //Creates a short pause before starting the new round
    private void startNewRoundDelayed() {
        int delay = 500; // milliseconds (0.5 seconds)
        // Create a new Timer that fires only once
        Timer delayTimer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // This runs after the delay
                if (balance > 0) {
                    startNewRound(); // Now it's safe to start the dealing process
                }
                ((Timer)e.getSource()).stop();// Stop the timer so it doesn't fire again
            }
        });
        
        // Set the timer to fire only once
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
    
    // GAME ENDS if player is bankrupt

    private void checkGameOver() 
    {
        if (balance <= 0) 
        {

            balance = 0; 
            hitButton.setEnabled(false);
            standButton.setEnabled(false);
            doubleButton.setEnabled(false);
            betButton.setEnabled(false);
            betSpinner.setEnabled(false);
            deckSelector.setEnabled(false);


            Timer delayTimer = new Timer(1500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) 
                {
                    
                    //ask the user if they want ot play again
                    int response = JOptionPane.showConfirmDialog(
                            BlackJackGUI.this, 
                            "GAME OVER! You ran out of money.\nDo you want to play again?", 
                            "Game Over", 
                            JOptionPane.YES_NO_OPTION);

                    if (response == JOptionPane.YES_OPTION) {
                        // --- restart ---
                        if (animationTimer.isRunning()) animationTimer.stop();
                        animationQueue.clear(); 
                        isDealerDrawing = false;
                        
                        balance = 10.00;
                        bet = 1.0;
                        betSpinner.setValue(1.0);
                        
                        startNewRound();
                    } else 
                    {
                        System.exit(0);
                    }
                }
            });
            
            delayTimer.setRepeats(false);
            delayTimer.start();
        }
    }

    // START NEW ROUND
    private void startNewRound() 
    {
        if (balance <= 0) return;

        roundOver = false;
        resultMessage = "";
        
        // Limpiar manos 
        player.resetHand();
        dealer.resetHand();
        
        hitButton.setEnabled(false);   // No puedes pedir carta aun
        standButton.setEnabled(false); // No puedes plantarte aun
        doubleButton.setEnabled(false);
        
        betButton.setEnabled(true);    // SI puedes apostar
        betSpinner.setEnabled(true);
        deckSelector.setEnabled(true);
        
        repaint();
    }
    
    private void processStand() 
    {
        // disable player actions immediately
        hitButton.setEnabled(false);
        standButton.setEnabled(false);
        betButton.setEnabled(false);
        doubleButton.setEnabled(false);
        
        // Reveal hidden card and START the animated dealer draw loop
        if (dealerHidden != null) dealerHidden.flipUp(); 
        
        // Dealer drawing now starts, managed by the animation timer
        dealerDrawLoop(); 
    }
    
    // --- Unified Dealing Methods ---
    private void dealPlayerAnimated(Deck deck, Player player) 
    {
    	dealCardAnimated(player, false); // always face up
    }
    
    private void dealDealerUpAnimated(Deck deck, Dealer dealer) {
    	dealCardAnimated(dealer, false); // face up
    }
    
    private Card dealDealerDownAnimated(Deck deck, Dealer dealer) {
    	dealCardAnimated(dealer, true); // face down
    	return null;
    }
    
    private void dealPlayerSilent(Deck deck, Player player) {
    	// This method is used when the player Hits, it should use the animation too.
        dealCardAnimated(player, false); 
    }
    
    // Setup for a new game
    private void startGame() {
    	// clear hands if this is not the first game
    	player.resetHand(); // Clear player hand
    	dealer.resetHand(); // Clear dealer hand
    	
    	int numDecks = (Integer) deckSelector.getSelectedItem();
    	deck = new Deck(numDecks);
       	deck.shuffle();
       	
       	//deal 4 initial cards
       	dealPlayerAnimated(deck, player);        // 1. Tú
    	dealDealerDownAnimated(deck, dealer);    // 2. Dealer (Oculta)
        dealPlayerAnimated(deck, player);        // 3. Tú
        dealDealerUpAnimated(deck, dealer);      // 4. Dealer (Visible)
        repaint(); // ensure screen is drawn with the initial cards
    }
    
    //CONSTRUCTOR
    public BlackJackGUI() 
    {
    	// timer fires every 20ms to update card position
    	animationTimer = new Timer(ANIMATION_SPEED, this::handleAnimationTick);
    	
    	player = new Player("Guest"); //temporal name for the player
    	    	  	    	
    	// deck selector
        deckSelector = new JComboBox<>(new Integer[]{1, 2, 4, 6});
        deckSelector.setSelectedItem(1); // default 1 deck  
        deckSelector.setFont(new Font("Arial", Font.BOLD, 15));
        deckSelector.setFocusable(false);
    	
        setTitle("Black Jack - Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        checkPlayerState();
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
        	    bet,         // Initial value
        	    1.0,         // Minimum bet allowed 
        	    9999.0,      // Placeholder for Max value 
        	    0.50         // Step size 
        );
        // Initialize the JSpinner
        betSpinner = new JSpinner(spinnerModel);
        // Set the format to display one or two decimal places for currency
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(betSpinner, "0.00");
        betSpinner.setEditor(editor);
        betSpinner.setFont(new Font("Arial", Font.BOLD, 15));
        
        //BUTTON TO CHANGE THEMES
        themeToggle = new JButton("🎄 Theme"); 
        themeToggle.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        themeToggle.setForeground(Color.WHITE);
        themeToggle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        // invisible button
        themeToggle.setFocusPainted(false);
        themeToggle.setBorderPainted(false);
        themeToggle.setContentAreaFilled(false);
        themeToggle.setOpaque(false);
        
       
        themeToggle.addActionListener(e -> {
            isXmasTheme = !isXmasTheme; // Invertir estado
            
            if (isXmasTheme) {
                // MODO NAVIDAD
                currentBgPath = "/finalProject/cards/background_xmas.png";
                currentBackPath = "/finalProject/cards/BACK_xmas.png";
                table_color = new Color(180, 20, 20); // Opcional: Rojo si falla la imagen
                //color de texto
                textColor =  Color.WHITE;
                // Cambiar el texto del botón para que ahora permita volver
                themeToggle.setText("♠️ Classic"); 
            } else {
                // MODO CLÁSICO
                currentBgPath = "/finalProject/cards/background.png";
                currentBackPath = "/finalProject/cards/BACK.png";
                table_color = new Color(53, 101, 77); 
                textColor = Color.WHITE;
                themeToggle.setText("🎄 Xmas");
            }
            deckLabel.setForeground(textColor);
            gamePanel.repaint(); 
        });
        
        
        // --- GAME PANEL (CENTER) ---
        gamePanel = new JPanel() 
        {
			private static final long serialVersionUID = 5225607520837154170L;

			@Override
            protected void paintComponent(Graphics g) 
            {
                super.paintComponent(g);
                
                // get background image URL
                java.net.URL backgroundURL = getClass().getResource(currentBgPath);
                Image background = new ImageIcon(backgroundURL).getImage();
                
                // draw background image first
                if (background != null) {
                    g.drawImage(background, 0, 0, getWidth(), getHeight(), null); // scaled to panel
                } else {
                    // fallback to solid felt color if image not found
                    g.setColor(table_color);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                
                // icon1 image
                java.net.URL icon1URL = getClass().getResource("/finalProject/cards/icon1.png");
                Image icon1 = new ImageIcon(icon1URL).getImage();
                g.drawImage(icon1, getWidth() - 1365, getHeight()-745, 300, 250, null); // X,Y,W,H
                
                // icon2 image
                java.net.URL icon2URL = getClass().getResource("/finalProject/cards/icon2.png");
                Image icon2 = new ImageIcon(icon2URL).getImage();
                g.drawImage(icon2, getWidth() - 290, getHeight()-725, 200, 220, null); // X,Y,W,H
                
                  
                // Get the currently animating card if one exists
                AnimationCard currentAnim = animationQueue.peek();
                                
                 // DRAW ANIMATED CARD (OVERLAYS ALL OTHER DRAWINGS)
                if (currentAnim != null) {
                    Card c = currentAnim.card;
                    Point p = currentAnim.current;
                    Image animCardImg;
                    
                    // Determine if we should show the face or the back
                    if (c.isFaceUp() || !currentAnim.isHoleCard) {
                        // Show face for player cards or if dealer card is flipped up
                        java.net.URL url = getClass().getResource(c.getImagePath());
                        animCardImg = new ImageIcon(url).getImage();
                    } else {
                        // Show back for hidden dealer card
                        java.net.URL url = getClass().getResource(currentBackPath);
                        animCardImg = new ImageIcon(url).getImage();
                    }
                    
                    // Draw the card at its CURRENT animated position (p.x, p.y)
                    g.drawImage(animCardImg, p.x, p.y, cardWidth, cardHeight, null);
                }
                
                // Define the shadow color (black with 40% transparency)
                g.setColor(new Color(0, 0, 0, 100)); // Alpha value 100 out of 255 (approx 40% opaque)
                int shadowOffset = 5; // Pixels to offset the shadow
                //to calculate the points of the dealer (only visible cards)

                
                // Draw Dealer Hand
                for(int i=0; i<dealer.getHandSize(); i++) 
                {
                    Card c = dealer.getCard(i);

                    // skip drawing this card if it's the one currently being animated
                    if (currentAnim != null && c.equals(currentAnim.card)) 
                    {
                        continue;
                    }

                	int drawX = 100 + i * (int)(cardWidth + 5);
                	int drawY = 230; 
                	
                	Image CardImg;
                	if(c.isFaceUp()) 
                	{
                		// show the card if is up
                		java.net.URL url2 = getClass().getResource(c.getImagePath());
                        CardImg = new ImageIcon(url2).getImage(); 
                	}else {
                		// show the back card if is not up
                		java.net.URL url2 = getClass().getResource(currentBackPath);
                        CardImg = new ImageIcon(url2).getImage();
                	}
                	
                	g.setColor(new Color(0, 0, 0, 150)); // shadow
                    g.fillRect(drawX + x + shadowOffset, drawY + shadowOffset, cardWidth, cardHeight);
                	
                    // Y=230 matches your original Y-position for dealer
                	g.drawImage(CardImg, drawX + x, drawY, cardWidth, cardHeight, null);
                }
                
                // DRAW PLAYER'S HAND
                for(int i=0; i<player.getHandSize(); i++) 
                {
                	Card c = player.getCard(i);
                    
                    // skip drawing this card if it's the one currently being animated
                    if (currentAnim != null && c.equals(currentAnim.card)) {
                       continue;
                    }
                    
                	int drawX = 100 + i * (cardWidth + 5);
                	int drawY = 450;
                	
                	Image CardImg;
                	java.net.URL url4 = getClass().getResource(c.getImagePath());
                    CardImg = new ImageIcon(url4).getImage();                        
                	
                    g.setColor(new Color(0, 0, 0, 150)); // shadow
                    g.fillRect(drawX + x + shadowOffset, drawY + shadowOffset, cardWidth, cardHeight);
                    
                    // Y=450 matches your original Y-position for player
                    g.drawImage(CardImg, drawX + x, drawY, cardWidth, cardHeight, null);
                }
                
                
                //CALCULATE THE DEALER'S SCORE WITH ITS VISIBLE CARDS 
	             int dealerVisibleScore = 0;
	             int dealerAces = 0;
	
	             for (int k = 0; k < dealer.getHandSize(); k++) 
	             {
	                 Card c = dealer.getCard(k);
	                 if (c.isFaceUp()) //only add the points for visible cards of the dealer
	                 {
	                     int val = c.getValue();
	                     if (val == 11) dealerAces++;
	                     dealerVisibleScore += val;
	                 }
	             }
	             // adjust aces if the sum>21
	             while (dealerVisibleScore > 21 && dealerAces > 0) 
	             {
	                 dealerVisibleScore -= 10;
	                 dealerAces--;
	             }
	
	             //draw the circle with the score
	             java.net.URL url3 = getClass().getResource("/finalProject/cards/score.png");
	             Image scoreImg = new ImageIcon(url3).getImage();
	             g.drawImage(scoreImg, 65 + x, 335, 90, 90, null); 
	
	             g.setFont(new Font("Arial", Font.BOLD, 18));
	             g.setColor(new Color(0,0,0,120)); // sombra
	             g.drawString("" + dealerVisibleScore, 100+2 + x, 385+2); 
	
	             g.setFont(new Font("Arial", Font.BOLD, 18));
	             g.setColor(Color.BLACK);
	             g.drawString("" + dealerVisibleScore, 100 + x, 385);
	             
                
                // draw player's score inside the circle image (score image)
                g.drawImage(scoreImg, 65 + x, 555, 90, 90, null);
                g.setColor(new Color(0,0,0,120)); // soft shadow
                g.drawString("" + player.getScore(), 100+2 + x, 605+2);
                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.setColor(Color.BLACK);
                g.drawString("" + player.getScore(), 100 + x, 605); // X, Y
                
                
                // coin image (for the balance)
                Image coinImg;
                java.net.URL url4 = getClass().getResource("/finalProject/cards/coin.png");
                coinImg = new ImageIcon(url4).getImage();
                int i=10;
                g.drawImage(coinImg, 70+x,  cardHeight + 495, 82+i, 76+i, null); // COIN IMAGE SIZE X,Y,H,W
                
                // Bet amount (inside coin image)
                int x=260, y=540;
                String betText = String.valueOf(bet);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 17)); 
                if(bet == 100.0) {
                	g.drawString("100", 38+x, cardHeight + y);
                }else {
                	if(bet <= 100 ) {
                    	g.setFont(new Font("Arial", Font.BOLD, 17));
                    	g.drawString(betText, 30+x, cardHeight + y); //X, Y 
                	}
                	else {
                		g.setFont(new Font("Arial", Font.BOLD, 15));
                    	g.drawString(betText, 36+x, cardHeight + y);
                	}
                }
                
                // ELEMENTS AT THE RIGHT TOP
                int p = 265;
                int m = 270;
                // show current balance amount 
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(Color.BLACK);
                g.setColor(new Color(0,0,0,120)); // soft shadow
                g.drawString("Bank: $" + balance, getWidth() - p + 2, 67+m);
                g.setColor(textColor);
                g.drawString("Bank: $" + balance, getWidth() - p, 67+m);
                
                // bet amount
                g.setColor(Color.BLACK);
                g.setColor(new Color(0,0,0,120)); // soft shadow
                g.drawString("Bet amount: ", getWidth() - p + 2, 160+m+10);
                //g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(textColor);
                g.drawString("Bet amount: ", getWidth() - p, 160+m+10);
                
                if (activePayoutMessage != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    PayoutMessage msg = activePayoutMessage;
                    
                    // Set the transparency and color
                    g2d.setColor(new Color(msg.color.getRed(), msg.color.getGreen(), msg.color.getBlue(), msg.alpha));
                    
                    // Set a large, bold font for impact
                    g2d.setFont(new Font("Arial", Font.BOLD, 36)); 
                    
                    // Draw the text at its current animating position
                    g2d.drawString(msg.text, msg.x, msg.y);
                }

                repaint();   
            }     
        };
        
        
        // --- BUTTON ACTIONS --- 
        //HIT button
        hitButton.addActionListener(e -> {
            hitButton.setEnabled(false);
            dealPlayerSilent(deck, player); 
            checkPlayerState();
            gamePanel.repaint();

        });
        
        //STAND 
        standButton.addActionListener(e -> 
        {
            processStand(); // Iniciar turno del dealer
            
        });

        // for the BET button
        betButton.addActionListener(new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) {
                // 1. Obtener valor del Spinner
                double newBet = ((Number) betSpinner.getValue()).doubleValue();
                
                // 2. Validaciones
                if (newBet <= 0.0) {
                    JOptionPane.showMessageDialog(BlackJackGUI.this, "Bet must be > 0.");
                    return;
                }
                if (newBet > balance) {
                    JOptionPane.showMessageDialog(BlackJackGUI.this, "Insufficient funds.");
                    betSpinner.setValue(balance); 
                    return;
                }

                // 3. APLICAR APUESTA
                bet = newBet;
                balance -= bet; 
                
                roundOver = false;
                
                // 4. BLOQUEAR CONTROLES DE APUESTA
                betButton.setEnabled(false);
                betSpinner.setEnabled(false);
                deckSelector.setEnabled(false);
                
                // 5. HABILITAR CONTROLES DE JUEGO
                hitButton.setEnabled(true);
                standButton.setEnabled(true);
                
                // 6. REPARTIR CARTAS
                startGame(); 
            }
        });
        
        // for the restart button
     // BOTÓN RESTART (MODIFICADO PARA FUNCIONAR SIEMPRE)
        restartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Preguntar confirmación (Opcional, para no borrar por error)
                int response = JOptionPane.showConfirmDialog(
                        BlackJackGUI.this, 
                        "¿Are you sure you want to RESTART the game?", 
                        "New Game", 
                        JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    if (animationTimer.isRunning()) animationTimer.stop();
                    animationQueue.clear(); 
                    isDealerDrawing = false;

                    balance = 10.00;  // Resetear dinero
                    bet = 1.0;        // Resetear apuesta
                    //betPlaced = false;
                    roundOver = true; // Forzar estado de fin de ronda
                    
                 
                    betSpinner.setValue(1.0); 
                    if (deckSelector.getItemCount() > 0) 
                    {
                        deckSelector.setSelectedIndex(0); 
                    }

               
                    startNewRound(); 
                }
            }
        });
        
        //DOUBLE BUTTON
        
        doubleButton.addActionListener(e -> {
            //check balance 
            if (balance < bet) {
                JOptionPane.showMessageDialog(BlackJackGUI.this, "Insufficient funds to Double!");
                return;
            }

            // 2. Aplicar la apuesta extra
            balance -= bet;         // Restar la apuesta original de nuevo
            bet = bet * 2;          // Duplicar el valor de la apuesta actual
            isDoubling = true;      // Marcar que estamos doblando

            // 3. Bloquear controles
            hitButton.setEnabled(false);
            standButton.setEnabled(false);
            doubleButton.setEnabled(false);

            // 4. Repartir LA ÚNICA carta extra
            dealPlayerSilent(deck, player);

            
            gamePanel.repaint();
        });
        
        
        
        // Deck label font
        deckLabel = new JLabel("Decks: ");
        deckLabel.setForeground(Color.WHITE);
        deckLabel.setFont(new Font("Arial", Font.BOLD, 20));

        gamePanel.setLayout(null); // allow manual placement of components
        
        standButton.addActionListener(this);
        
        // Add controls to the panel before showing
        gamePanel.add(hitButton);
        gamePanel.add(standButton);
        gamePanel.add(doubleButton);
        gamePanel.add(betButton);
        gamePanel.add(restartButton);
        gamePanel.add(betSpinner);
        gamePanel.add(deckLabel);
        gamePanel.add(deckSelector);
        gamePanel.add(themeToggle);

        setContentPane(gamePanel);
        
        pack(); // Pack the window and validate components BEFORE going full screen
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // A helper to place controls using the panel’s current size
        Runnable positionControls = () -> {
            int w = gamePanel.getWidth();
            int h = gamePanel.getHeight();
            if (w <= 0 || h <= 0) return; // not laid out yet

            int btnW = 120, btnH = 40, gap = 20;
            int y = h - btnH - 20; // 20px above bottom
            int cx = w / 2;
            
            int totalClusterWidth = (5 * btnW) + (4 * gap); // Total width of the 5-button cluster + 4 gaps
            int startX = cx - (totalClusterWidth / 2); // Start X position is the center minus half the cluster width

            // place HIT, STAND, DOUBLE, BET, and EXIT buttons (bottom center)
            hitButton.setBounds(startX, y, btnW, btnH);
            standButton.setBounds(startX + btnW + gap, y, btnW, btnH);
            doubleButton.setBounds(startX + 2 * (btnW + gap), y, btnW, btnH);
            betButton.setBounds(startX + (3 * (btnW + gap)), y, btnW, btnH);
            restartButton.setBounds(startX + (4 * (btnW + gap)), y, btnW, btnH);

             int p = 165;
             int m = 270;
            // bet amount field
            //betField.setBounds(getWidth() - p + 5, 140 + m + 10, 80, 28); // X, Y, W, H
            betSpinner.setBounds(getWidth() - p + 5, 140 + m + 10, 80, 28); // X, Y, W, H
            deckSelector.setBounds(getWidth() - p + 5, 100 + m, 60, 28); // x, y, h w
            deckLabel.setBounds(getWidth() - p - 116, 100 + m, 80, 28);
            //theme button --> upper right
            themeToggle.setBounds(getWidth() - 140, 50, 120, 30);
        };

        // Position once after the window is realized
        SwingUtilities.invokeLater(positionControls);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { positionControls.run(); }
        });
        
        setVisible(true); // show after everything is added

        startNewRound();
    } // end of BlackJackGUI()

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint(); // redraw if needed
    }
    

    public static void main(String[] args) {
        //show pop ups in english
        java.util.Locale.setDefault(java.util.Locale.US); 
        
        SwingUtilities.invokeLater(() -> new BlackJackGUI());
    }
} 