# Events

This is an event management mobile application that allows users to create, manage, and share events. The app features a user-friendly interface for authentication, event creation, and profile management.

## Table of Contents

- [Overview](#overview)
- [Technologies](#technologies)
- [Features](#features)
- [Setup](#setup)
- [Contributions](#contributions)
- [License](#license)

## Overview

Events is designed to streamline the process of event creation and management. It provides an intuitive user interface to sign in, create events, track proposals, and manage personal profiles.

## Technologies

- **Kotlin**: Primary programming language for Android development.
- **Supabase**: Used for user authentication (email, Google, and social login options).
- **Android SDK**: Core framework for Android development.
- **Ktor**: Client for network requests.
- **Glide**: For image loading and display.
- **Lottie**: For animations.
- **Jetpack Navigation**: For handling navigation within the app.
- **Dotenv Kotlin**: For managing environment variables (e.g., Supabase credentials).
- **Google Play Services**: For authentication and location services.

## Features

- **User Authentication:**
    - Login using Google, Facebook or X (Twitter).
    - Email verification with a unique code.

- **Event Creation:**
    - Create events by adding a name, description, time, tags, and location.
    - Option to add images to events.

- **Event Management:**
    - Track and edit events, including proposal details.
    - Filter events based on date, location, and tags (e.g., meetings).

- **User Profile:**
    - View and edit personal details, including username and social media links.
    - Option to delete the account.

- **Error Handling:**
    - Displays error screens for issues such as no internet connection.

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/Sangariusss/Events.git
   ```

2. Open the project in Android Studio.

3. Build the project to download dependencies.

4. Set up **Supabase** for authentication:
    - Create a Supabase account and project at [https://supabase.com/](https://supabase.com/).
    - Configure Supabase with the authentication services you want (email, Google, etc.).
    - Add your Supabase URL and API key to your project’s `.env` file.

5. Run the app on an emulator or real device.

## Contributions

We welcome contributions! If you'd like to improve the app or fix bugs, feel free to open a pull request. Please follow the coding guidelines and write clear commit messages.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
