package finalProject;

import java.util.Scanner;

import javax.swing.SwingUtilities;

public class Main 
{

    // method to deal a card to the PLAYER
    private static void dealPlayer(Deck deck, Player player) 
    {
        Card c = deck.deal();
        player.addCard(c);
        c.flipUp(); // reveal immediately for console game
    }
    
   /* private static void dealPlayerSilent(Deck deck, Player player) 
    {
        Card c = deck.deal();
        player.addCard(c);
        c.flipUp();  // reveal; we print below
    }*/
    
    //Methods to deal card to DEALER
    //deal card UP to the dealer
    private static void dealDealerUp(Deck deck, Dealer dealer) 
    {
        Card c = deck.deal();
        dealer.addCard(c); //inherited method
        c.flipUp(); // dealer upcard
    }
    //deal a card face DOWN to the dealer
    private static Card dealDealerDown(Deck deck, Dealer dealer)
    {
        Card c = deck.deal(); // keep face-down
        dealer.addCard(c);
        return c; // Return it so we can flip it later
    }


    //calculate winner and print results
    private static double gameEnds(Player player, Dealer dealer, Card dealerHidden,
                                   int bet, double balance) 
    {
        // Hidden card was flipped earlier; banner only
        System.out.println();
        System.out.println("************************************************");
     
        System.out.println("DEALER's second card is: " + dealerHidden.fullName()
        + " [Score: " + dealerHidden.getValue() + "]");

        System.out.println("************************************************");
        System.out.println();
        
        //use getters to get scores of dealer and player
        int dealerScore = dealer.getScore();
        int playerScore = player.getScore();

        System.out.println("YOUR total score: " + playerScore);
        System.out.println("DEALER's total score: " + dealerScore);
        
        //win/loss conditions
        if (playerScore > 21) 
        {
            System.out.println("YOU lose 😭");
            balance -= bet;
        } else if (dealerScore > 21) {
            System.out.println("YOU win 🏆");
            balance += bet;
        } else if (playerScore == 21 && player.getHandSize() == 2) {
            System.out.println("Blackjack! You win 2.5x bet!");
            balance += bet * 2.5;  // net +1.5×
        } else if (playerScore > dealerScore) {
            System.out.println("YOU win 🏆");
            balance += bet;
        } else if (playerScore < dealerScore) {
            System.out.println("YOU lose 😭");
            balance -= bet;
        } else {
            System.out.println("Tie 🤝");
        }

        System.out.println("Current balance: " + balance);
        return balance;
    }

    // ----- Main -----
    public static void main(String[] args) 
    {
    	
    	SwingUtilities.invokeLater(() -> new BlackJackGUI()); // launches the GUI
        Scanner sc = new Scanner(System.in);

        System.out.println("🃏♠️ Welcome to Blackjack 🃏♠️");
        //input decks
        System.out.println("Enter number of decks (1-4): ");
        System.out.print("👉  ");
        int numDecks = sc.nextInt();
        sc.nextLine();  // consume newline
        if (numDecks < 1) numDecks = 1;
        if (numDecks > 4) numDecks = 4;

        Deck deck = new Deck(numDecks);
        deck.shuffle();

        double balance = 100.0;
        System.out.println("Initial balance: " + balance);
        
        //main game loop
        while (balance > 0.0) 
        {
            System.out.print("\nEnter your BET amount: ");
            int bet = sc.nextInt();
            sc.nextLine();
            if (bet <= 0 || bet > balance) {
                System.out.println("Invalid bet. Try again… 💰");
                continue;
            }
            
            //instantiate dealer and player objects
            Player player = new Player("Guest");
            Dealer dealer = new Dealer();

            // Initial deal: P up, D up, P up, D down
            dealPlayer(deck, player); //deals first card to player
            dealDealerUp(deck, dealer); //deal first card of dealer
            dealPlayer(deck, player); //deal second card of player
            Card dealerHidden = dealDealerDown(deck, dealer); //deal second card (hidden) for dealer

         // Print Hands using Encapsulation methods
            System.out.print("\nCARDS DEALT TO YOU ARE: ");
            //iterate through the player hand
            for (int i = 0; i < player.getHandSize(); i++) 
            {
                System.out.print(player.getCard(i).fullName()); //get a  card
             // Print " and " if it's not the last card, otherwise print space
                System.out.print(i < player.getHandSize() - 1 ? " and " : " ");
            }
            //print player total score
            System.out.println("[TOTAL SCORE: " + player.getScore() + "]");

            System.out.print("DEALER'S CARDS: ");
            // Show only the first card of the dealer and hide the second
            System.out.println(dealer.getCard(0).fullName() + " and [hidden card]\n");

            // ----- Player's turn loop -----
            while (true) 
            {
                int pScore = player.getScore();
                if (pScore >= 21) break; // Check if player busted or got 21 

                System.out.println("WOULD YOU LIKE TO hit or stand?");
                System.out.print("👉  ");
                //read user action (hit or stand)
                String action = sc.next().trim().toLowerCase();
                sc.nextLine(); // flush rest of line

                if (action.equals("hit")) 
                {
                    System.out.println("Your action: hit");
                    dealPlayer(deck, player); //deal a card to the player
                    // Access the last card added to show what they drew
                    Card last = player.getCard(player.getHandSize()- 1);
                    System.out.println("You drew: " + last.fullName()
                            + " [TOTAL SCORE: " + player.getScore() + "]");
                } 
                else if (action.equals("stand")) 
                {
                    System.out.println("Your action: stand");
                    break;
                } 
                else 
                {
                    System.out.println("Please type 'hit' or 'stand'.");
                }
            }
            
            // --- Dealer's Turn ---
            // ----- Reveal hidden card BEFORE dealer draws -----
            if (dealerHidden != null && !dealerHidden.isFaceUp()) 
            {
                dealerHidden.flipUp(); //flip the hidden card
            }

            // ----- Dealer draws to 17 -----
            dealer.autoDraw(deck);

            // Calculate results and update balance
            balance = gameEnds(player, dealer, dealerHidden, bet, balance);

            if (balance <= 0.0) 
            {
                System.out.println("GAME OVER! You are out of money.");
                break;
            }
            System.out.print("Play again? (y/n): ");
            String again = sc.next().trim().toLowerCase();
            sc.nextLine(); // flush
            if (!again.equals("y")) 
            {
                System.out.println("Thanks for playing! Final balance: " + balance);
                break;
            }
        }

        sc.close();
    }
}
