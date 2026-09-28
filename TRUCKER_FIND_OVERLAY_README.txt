TRUCKER FIND - FLOATING QUICK ACCESS UPDATE

Added:
- Android display-over-other-apps permission flow.
- Foreground overlay service for Android 8+ / target SDK 36.
- Draggable TF button on the right side of the screen.
- Tap TF to expand/collapse quick-stop buttons.
- Fuel, Food, Truck Parking, Truck Repair, Laundry, Truck Wash, Hotels, 24/7 Truck Stop, Chrome Shop.
- Weigh Stations uses the colored semi-on-platform-scale icon from the Trucker Find design.
- Each button opens a Google Maps search and then collapses the menu.
- X button stops the floating overlay.

FIRST TEST:
1. Build/install this Android project.
2. Launch Trucker Find.
3. Android should open "Display over other apps" settings once.
4. Enable "Allow display over other apps" for Trucker Find.
5. Return to Trucker Find. The TF floating button starts.
6. Open Google Maps/navigation and test the TF button.

NOTE:
A local compile was attempted, but this environment could not download the Gradle 9.5.1 distribution because outbound network access to services.gradle.org is unavailable. The source modifications are packaged for building in an Android/Gradle environment with network access.
