package finalProject;

//Dealer is a child of Player --> inherits  hand, name, addCard(), getScore()
public class Dealer extends Player
{
	//constructor
	public Dealer()
	{
		super("Dealer"); //pass Dealer to the parent's constructor
	}
	
	// Casino Rule: The dealer must draw cards as long as they have less than 17
	// ----- Dealer draws to 17 (after hidden card is revealed) -----
	public void autoDraw(Deck deck) 
	{
	    while (this.getScore() < 17) 
	    { 
	        Card c = deck.deal(); //ask deck for a card
	        this.addCard(c); // Dealer adds the new card to their hand
	        c.flipUp(); //flip the card
	        System.out.println("Dealer draws: " + c.fullName() + 
	                           " [score: " + this.getScore() + "]");
	    }
	}	

}
