# Financial CRM & Reception Management System

**Enterprise-grade Financial CRM built with Spring Boot 3.x, Java 21, MySQL 8, Thymeleaf & Bootstrap 5**

---

## 📋 Table of Contents
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Default Login](#default-login)
- [Folder Structure](#folder-structure)
- [Database](#database)
- [API Endpoints](#api-endpoints)
- [Security](#security)

---

## Overview
A complete financial services CRM for managing customer inquiries, loan processing, insurance, follow-ups,
appointments, employees, attendance, income/expense tracking, and automated reminders.

---

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.x |
| Security | Spring Security 6 |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.0 |
| Build | Maven |
| Frontend | Thymeleaf + Bootstrap 5.3 |
| Charts | Chart.js 4 |
| Excel | Apache POI 5.x |
| Scheduler | Spring Scheduler |

---

## Modules
1. **Authentication** – Login, Logout, Forgot Password, Change Password, Role-Based Access
2. **Customer Management** – Customer entry, search, birthday/anniversary tracking
3. **Inquiry Management** – Home Loan, LAP, Personal, Business Loan, Insurance, MF, Credit Card, CIBIL
4. **Appointment Management** – Daily meeting list, scheduling, status tracking
5. **CRM Follow-up** – Follow-up tracking, WhatsApp/call reminders, overdue alerts
6. **Employee Management** – Employee records with photo upload, bank details
7. **Attendance Management** – Daily marking, bulk mark, monthly reports
8. **Dashboard** – KPI widgets, charts, birthday reminders, today's follow-ups
9. **WhatsApp/SMS** – API-ready integration architecture
10. **Calculators** – EMI, SIP, SWP, Insurance Premium
11. **Reports** – Excel exports for Inquiries, Expenses, Income, Attendance
12. **Expense Management** – 6 categories, payment modes, vendor tracking
13. **Income Management** – 10 income categories, payment tracking
14. **Utility Bill Reminders** – Due-date tracking, reminder alerts, manual and online payment support
15. **Notifications** – In-app notifications, scheduled reminders
16. **Audit Logging** – Full user action trail

---

## Prerequisites
- **Java 21** (JDK)
- **Maven 3.9+**
- **MySQL 8.0+**
- **Node.js** (optional, for frontend assets)

---

## Installation

### 1. Clone / Extract the project
```bash
cd financial-crm
```

### 2. Create MySQL Database
```sql
CREATE DATABASE financial_crm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Run the Schema
```bash
mysql -u root -p financial_crm < src/main/resources/db/schema.sql
```

### 4. Update application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/financial_crm?...
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 5. Build & Run
```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

### 6. Access the Application
```
http://localhost:8080
```

---

## Default Login
| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `Admin@123` |

> ⚠️ **Change the default password immediately after first login!**

---

## Folder Structure
```
financial-crm/
├── src/
│   ├── main/
│   │   ├── java/com/financialcrm/
│   │   │   ├── config/          # Security, App config
│   │   │   ├── controller/      # MVC + REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── exception/       # Custom exceptions + global handler
│   │   │   ├── repository/      # Spring Data JPA repos
│   │   │   ├── service/         # Service interfaces + impls
│   │   │   └── util/            # Utility classes
│   │   └── resources/
│   │       ├── db/schema.sql    # Full database schema + seed data
│   │       ├── static/
│   │       │   ├── css/app.css
│   │       │   └── js/app.js
│   │       ├── templates/       # Thymeleaf HTML templates
│   │       └── application.properties
│   └── test/
└── pom.xml
```

---

## Database
**19 Tables:**
- `users`, `roles`, `user_roles`
- `customers`
- `inquiry_types`, `inquiries`
- `appointments`
- `followups`
- `employees`, `attendance`
- `expense_categories`, `expenses`
- `income_categories`, `income`
- `notifications`, `audit_logs`
- `insurance_policies`, `emi_tracker`, `message_logs`

---

## API Endpoints

### Customer API
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/customers/search?q=` | Live customer search |
| GET | `/customers` | List all customers |
| POST | `/customers/save` | Create customer |
| GET | `/customers/{id}` | View customer |
| POST | `/customers/{id}/update` | Update customer |

### Inquiry API
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/inquiries` | List inquiries |
| POST | `/inquiries/save` | Create inquiry |
| GET | `/inquiries/{id}` | View inquiry |

### Reports API
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/reports/inquiries/excel?startDate=&endDate=` | Excel report |
| GET | `/reports/expenses/excel?startDate=&endDate=` | Excel report |
| GET | `/reports/income/excel?startDate=&endDate=` | Excel report |

### Calculator API
| Method | URL | Params |
|--------|-----|--------|
| POST | `/calculator/emi` | principal, annualRate, tenureMonths |
| POST | `/calculator/sip` | monthlyInvestment, annualReturn, tenureYears |
| POST | `/calculator/swp` | corpus, monthlyWithdrawal, annualReturn |
| POST | `/calculator/insurance` | sumAssured, age, tenureYears, type |

---

## Security
- **BCrypt** password encoding (strength 12)
- **CSRF** protection enabled (Cookie-based)
- **XSS** protection headers
- **Session** management with max 1 concurrent session
- **Role-based** method security (`@PreAuthorize`)
- **SQL Injection** prevention via parameterized JPA queries
- **Account lockout** after failed attempts (configurable)

---

## Scheduled Jobs
| Time | Job |
|------|-----|
| 7:00 AM daily | Birthday reminders |
| 8:00 AM daily | Follow-up reminders |
| 8:30 AM daily | Overdue follow-up alerts |

---

## Utility Bill Payments
The Utility Bills module supports two payment flows:

- **Manual payment recording** from the bill list using `Cash`, `UPI`, `Bank Transfer`, `Cheque`, or `Card`
- **Online payment via PhonePe** when merchant credentials are configured

### Enable PhonePe Online Payments
1. Complete merchant onboarding with PhonePe.
2. Collect these values from the PhonePe dashboard:
   - `merchant-id`
   - `salt-key`
   - `salt-index`
3. Update `src/main/resources/application.properties`:

```properties
app.payment.phonepe.enabled=true
app.payment.phonepe.base-url=https://api-preprod.phonepe.com/apis/pg-sandbox
app.payment.phonepe.merchant-id=YOUR_MERCHANT_ID
app.payment.phonepe.salt-key=YOUR_SALT_KEY
app.payment.phonepe.salt-index=1
```

4. Keep `app.base-url` aligned with the public URL of your app so PhonePe can return users to the correct bill-payment page.
5. Start with the sandbox URL and switch to the production base URL only after PhonePe approves your live merchant account.

> Never commit real merchant credentials to version control. Use environment-specific overrides for production.

---

## WhatsApp / SMS
The system is **API-ready** for WhatsApp and SMS integration.
Configure the provider endpoints in `application.properties`:
```properties
app.whatsapp.api.url=https://your-provider/send
app.whatsapp.api.key=your-key
app.sms.api.url=https://your-sms-provider/send
app.sms.api.key=your-key
```

### How To Create Twilio API Keys
The current WhatsApp integration uses Twilio credentials:

```properties
twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.auth.token=YOUR_AUTH_TOKEN
twilio.whatsapp.from=whatsapp:+14155238886
```

Use this setup flow:
1. Create an account at [Twilio](https://www.twilio.com/).
2. Copy the **Account SID** from the Twilio Console home page.
3. Reveal or regenerate the **Auth Token** from your Twilio account settings.
4. Open **Messaging > Try it out > Send a WhatsApp message** and enable the WhatsApp Sandbox.
5. Copy the sandbox sender number and join code.
6. Paste the SID, token, and sender number into `application.properties` or environment overrides.
7. Ask each test user to send the sandbox join message before you trigger WhatsApp reminders from the CRM.

For a custom provider, you can still populate:

```properties
app.whatsapp.api.url=https://your-provider/send
app.whatsapp.api.key=YOUR_PROVIDER_API_KEY
app.sms.api.url=https://your-sms-provider/send
app.sms.api.key=YOUR_SMS_PROVIDER_API_KEY
```

---

## Production Deployment

### Build JAR
```bash
mvn clean package -Pprod -DskipTests
java -jar target/financial-crm-1.0.0.jar --spring.profiles.active=prod
```

### Systemd Service (Linux)
```ini
[Unit]
Description=Financial CRM
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/financial-crm
ExecStart=/usr/bin/java -Xmx512m -jar financial-crm-1.0.0.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

### Nginx Reverse Proxy
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## License
Proprietary – All rights reserved.

---

*Built with ❤️ using Spring Boot 3 + Java 21*
