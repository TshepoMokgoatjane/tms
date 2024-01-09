package za.co.tms.model;

public enum Room {

	A1 ("One Bedroom Apartment"),
	A2 ("Two Bedroom Apartment"),
	A3 ("One Bedroom Apartment"),
	A4 ("One Bedroom Apartment"),
	A5 ("One Bedroom Apartment"),
	A6 ("One Bedroom Apartment"),
	A7 ("One Bedroom Apartment"),
	A8 ("One Bedroom Apartment"),
	A9 ("One Bedroom Apartment"),
	A10 ("One Bedroom Apartment"),
	A11 ("Two Bedroom Apartment"),
	B1 ("Two Bedroom Apartment"),
	B2 ("Two Bedroom Apartment"),
	C1 ("Bachelor Room");	
	
	private String roomDescription;
	
	private Room(String roomDescription) {
		this.roomDescription = roomDescription;
	}
	
	public static void main(String[] args) {
		System.out.println("Show me A11 Room: " + A11.roomDescription);
	}
	
}
