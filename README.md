# Real Estate Management System

A comprehensive real estate management platform built with Spring Boot and modern web technologies.

## Features

- User authentication and role-based access control (User, Realtor, Admin)
- Property listing and search with filtering options
- Property details with image galleries and maps
- User profile management
- Realtor property management
- Wishlist functionality
- Transaction history
- Responsive design for all devices

## Technology Stack

- **Backend**: Java 17, Spring Boot, Spring Security, Spring Data JPA
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5, Tailwind CSS
- **Database**: MySQL/PostgreSQL (configurable)
- **Security**: JWT Authentication
- **Others**: RESTful API, Responsive Design

## Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── configuration/     # Application configuration
│   │   ├── controller/        # REST controllers and MVC controllers
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── exception/         # Custom exceptions and handlers
│   │   ├── model/             # JPA entities
│   │   ├── repository/        # JPA repositories
│   │   └── service/           # Business logic services
│   └── resources/
│       ├── static/            # Static resources (CSS, JS, images)
│       │   └── assets/
│       │       ├── css/       # Stylesheets
│       │       ├── js/        # JavaScript files
│       │       └── images/    # Image resources
│       ├── templates/         # Thymeleaf templates
│       └── application.properties # Application configuration
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- MySQL or PostgreSQL database

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/real-estate.git
   cd real-estate
   ```

2. Configure the database connection in `src/main/resources/application.properties`

3. Build and run the application:
   ```
   mvn clean install
   mvn spring-boot:run
   ```

4. Access the application at http://localhost:8081

## User Roles

- **User**: Can browse properties, create wishlists, and view their profile
- **Realtor**: Can manage their property listings and view customer inquiries
- **Admin**: Has access to all system features and user management

## Security

The application uses JWT (JSON Web Tokens) for authentication. All sensitive endpoints are protected, requiring valid authentication tokens.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Bootstrap 5 for the responsive UI components
- Spring Boot for the robust backend framework
- All contributors who have helped build this project 