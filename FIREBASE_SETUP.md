# Firebase Google Login Setup Instructions

## Important: Web Client ID Configuration Required

To complete the Google login setup, you need to configure the Web Client ID from your Firebase project.

### Steps to get your Web Client ID:

1. **Go to Firebase Console**
   - Visit https://console.firebase.google.com/
   - Select your project "bagetmamanger-2"

2. **Navigate to Authentication**
   - Click on "Authentication" in the left sidebar
   - Go to "Sign-in method" tab
   - Enable "Google" as a sign-in provider

3. **Get Web Client ID**
   - In the Google sign-in configuration, you'll see your Web Client ID
   - Copy the Web Client ID (it looks like: `123456789-abcdefg.apps.googleusercontent.com`)

4. **Update the code**
   - Open `LoginActivity.java`
   - Replace `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID:
   ```java
   private static final String WEB_CLIENT_ID = "your-actual-web-client-id.apps.googleusercontent.com";
   ```

### Alternative: Using string resources (Recommended)

1. Add to `app/src/main/res/values/strings.xml`:
```xml
<string name="web_client_id">your-actual-web-client-id.apps.googleusercontent.com</string>
```

2. Update `LoginActivity.java`:
```java
private static final String WEB_CLIENT_ID = getString(R.string.web_client_id);
```

### Testing the Login Flow

1. Build and run the app
2. The LoginActivity should appear first
3. Tap "Sign in with Google"
4. Complete Google authentication
5. You should be redirected to the main Budget Manager interface
6. Use the logout option in the menu to test the full flow

### Troubleshooting

- Make sure your `google-services.json` file is up to date
- Verify that the package name matches your Firebase project configuration
- Check that Google Sign-in is enabled in Firebase Console
- Ensure your app's SHA-1 fingerprint is added to Firebase (for release builds)