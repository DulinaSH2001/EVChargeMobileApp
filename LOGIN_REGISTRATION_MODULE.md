# VoltVoyage - Login & Registration Module

## Overview

Complete implementation of the Login & Registration Module with local and remote API integration for VoltVoyage.

## Features Implemented

### 1. Enhanced User Data Model

- **User.kt**: Added support for user types (EV_OWNER, STATION_OPERATOR), account status, and server sync tracking
- **UserType enum**: Distinguishes between EV Owners and Station Operators
- **Account status management**: Track active/inactive users
- **Sync tracking**: Monitor which users need server synchronization

### 2. Local Database Schema

- **Updated UserDbHelper.kt**: Enhanced SQLite schema with new fields
- **Database version upgrade**: Handles migration from version 1 to 2
- **New methods**: User validation, sync management, account deactivation
- **Backward compatibility**: Maintains existing functionality

### 3. API Configuration & Services

- **ApiConfig.kt**: Retrofit setup with OkHttp, logging, and error handling
- **AuthApiService.kt**: RESTful API interface for authentication
- **API Models**: Request/response data classes for server communication
- **AuthRepository.kt**: Business logic layer managing local and remote data

### 4. Network & Offline Management

- **NetworkUtils.kt**: Network connectivity detection and status monitoring
- **OfflineManager.kt**: Handles offline scenarios and data synchronization
- **Automatic sync**: Background synchronization when network becomes available

### 5. Enhanced Session Management

- **SessionManager.kt**: Comprehensive session handling with new features
- **User type tracking**: EV Owner vs Station Operator sessions
- **Account status validation**: Prevent login for deactivated accounts
- **Session timeout**: Automatic re-authentication after 24 hours
- **Backward compatibility**: Maintains existing session data

### 6. Upgraded Activities

#### LoginActivity.kt

- **Remote API integration**: Primary authentication via server
- **Offline fallback**: Local authentication when network unavailable
- **Account status validation**: Blocks deactivated account logins
- **Enhanced UI**: Progress indicators, status messages, loading states
- **Error handling**: Comprehensive error messages and user feedback

#### RegisterActivity.kt

- **EV Owner registration**: Only EV Owners can self-register
- **Enhanced validation**: NIC format, email, phone number validation
- **Remote API calls**: Server registration with local backup
- **Sync capabilities**: Automatic background sync when connected
- **Improved UX**: Loading states, better error messages

## API Endpoints

### Authentication Endpoints

```
POST /api/login
- Body: { nic: string, password: string }
- Response: { success: boolean, message: string, user: ApiUser, token: string }

POST /api/evowners/register
- Body: { nic: string, name: string, email: string, phone: string, password: string }
- Response: { success: boolean, message: string, user: ApiUser }

GET /api/users/validate/{nic}
- Response: { success: boolean, exists: boolean, isActive: boolean, message: string }

POST /api/users/sync
- Body: ApiUser object
- Response: { success: boolean, message: string }
```

## Login Flow

### For EV Owners

1. User enters NIC and password
2. System attempts remote authentication
3. If successful, updates local user data
4. If network unavailable, falls back to local authentication
5. Validates account status (active/inactive)
6. Creates user session and redirects to dashboard

### For Station Operators

1. Operators use existing accounts (created via web app)
2. Same authentication flow as EV Owners
3. Quick "Operator Access" button for testing

## Registration Flow

### EV Owner Registration

1. User fills registration form with validation
2. System checks for internet connection (required)
3. If no internet, registration fails with error message
4. Attempts remote registration via API
5. If successful, creates user session with API data
6. Redirects to dashboard without local database storage
7. No local fallback - API-only registration

### Station Operator Registration

- Station Operators cannot self-register
- Accounts created via web application
- Can only login through the mobile app

## Account Status Management

### Active Accounts

- Users can login and access all features
- Regular operation and functionality

### Deactivated Accounts

- Login blocked with appropriate message
- Local users: "Account is deactivated. Please connect to internet to verify status."
- Remote validation: "Account is deactivated. Please contact support."

## Offline Support

### Login Support

- Local authentication available when network unavailable
- Background sync when network becomes available for existing users

### Registration Limitations

- **New registrations require internet connection**
- No offline registration capability
- Users must be online to create new accounts

### Sync Management

- Tracks which existing users need server synchronization
- Automatic sync on network reconnection for logged-in users
- Status messages inform users about sync status

## Test Credentials

### Pre-loaded Test Users

```
EV Owner 1:
- NIC: test
- Password: 123

EV Owner 2:
- NIC: demo
- Password: demo

Station Operator:
- NIC: operator123
- Password: operator
```

## Dependencies Added

- Retrofit 2.9.0 (HTTP client)
- OkHttp 4.11.0 (Network layer)
- Gson converter (JSON parsing)
- Coroutines (Async operations)

## UI Enhancements

- Progress bars during authentication
- Status text for network/sync information
- Loading states for buttons
- Enhanced error messaging
- Improved validation feedback

## Error Handling

- Network connectivity issues
- API server errors
- Validation failures
- Account status restrictions
- Session timeout scenarios

## Security Considerations

- Passwords stored locally for offline access (consider encryption in production)
- Session timeout after 24 hours
- Account status validation
- Network security with HTTPS

## Future Enhancements

1. Password encryption for local storage
2. Biometric authentication
3. Two-factor authentication
4. Password reset functionality
5. Social media login integration

## Configuration

Update `ApiConfig.kt` with your actual server URL:

```kotlin
private const val BASE_URL = "https://your-api-server.com/api/"
```
