package za.co.tms.model;

import lombok.Getter;

public enum Room {

	A1 ("One Bedroom Apartment", "Meter Mate - 07142460315"),
	A2 ("Two Bedroom Apartment", "Meter Mate - 07139265537"),
	A3 ("One Bedroom Apartment", "Meter Mate - 07137656497"),
	A4 ("One Bedroom Apartment", "CitiQ - 07162631308"),
	A5 ("One Bedroom Apartment", "CitiQ - 07131811031"),
	A6 ("One Bedroom Apartment", "CitiQ - 07131810959"),
	A7 ("One Bedroom Apartment", "Meter Mate - 07125412432"),
	A8 ("One Bedroom Apartment", "Meter Mate - 07164984812"),
	A9 ("One Bedroom Apartment", "CitiQ - 07131811023"),
	A10 ("One Bedroom Apartment", "CitiQ - 07131811049"),
	A11 ("Two Bedroom Apartment", "Meter Mate - 07164984853"),
	B1 ("Two Bedroom Apartment", "Meter Mate - 07164984838"),
	B2 ("Two Bedroom Apartment", "Meter Mate - 07139265529"),
	C1 ("Bachelor Room", "Meter Mate - 07164984820"),
    ERF490 ("Two Bedroom House", "Eskom - 45006087326");
	
	@Getter
    private final String roomDescription;

    @Getter
    private final String meterNumber;
	
	Room(String roomDescription, String meterNumber) {
        this.roomDescription = roomDescription;
        this.meterNumber = meterNumber;
	}

    public static void main(String[] args) {
		System.out.println("Show me A11 Room: " + A11.roomDescription + " and its meter number: " + A11.meterNumber);
	}

    @Override
    public String toString() {
        return name() + " - " + roomDescription;
    }
}