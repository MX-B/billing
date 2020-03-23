Payments - Billing
=====================

This project does the billing part 

## Environment Configuration

| ENV Name                                  | Description                                                                   | Required | Default Value                                                                                               |
|-------------------------------------------|-------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------|
| `JDBC_CONNECTION_STRING`                  | Used to create connection with the database                                   | **Yes**  | `jdbc:mysql://localhost:3306/billing?useLegacyDatetimeCode=false&useTimezone=true&serverTimezone=UTC`  |
| `DB_USER`                                 | MySQL Database User                                                           | No       | `<blank>`                                                                                                   |
| `DB_PASSWORD`                             | MySQL Database Password                                                       | No       | `<blank>`                                                                                                   |
| `PORT`                                    | TCP Port where the server will run                                            | No       | `8080`                                                                                                      |
| `LOG_LEVEL`                               | Log level                                                                     | No       | `INFO`                                                                                                      |
| `AUTH_CLIENT_SECRET`                      | Secret to authenticate with Keycloak                                          | **Yes**  |                                                                                                             |
| `AUTH_CLIENT`                             | ClientID to authenticate with Keycloak                                        | No       | `billing`                                                                                     |
| `AUTH_REALM`                              | Realm to authenticate with Keycloak                                           | No       | `master`                                                                                                    |
| `AUTH_SERVER_URL`                         | Keycloak Auth URL                                                             | No       | ``                                                                               |
| `PAGARME_APIKEY`                          | Pagar.me API Key                                                              | **Yes**  | `<blank>`                                                                                                   |
| `PAGARME_ENCKEY`                          | Pagar.me Enc Key                                                              | **Yes**  | `<blank>`                                                                                                   |
| `SENDGRID_API_KEY`                        | Sendgrid API Key                                                              | **Yes**  | `<blank>`                                                                                                   |
| `DEFAULT_RECIPIENT_UUID`                  | Default Recipient to receive Split                                            | **Yes**  | `<blank>`                                                                                                   |
| `SERVICE_SUBSCRIPTIONS`                   | Subscriptions Service URL                                                     | No       | ``                                                                     |
| `SERVICE_RECIPIENTS`                      | Recipients Service URL                                                        | No       | ``                                                                        |
| `SERVICE_NOTIFICATION`                    | Notification Manager URL                                                      | No       | ``                                                                  |
| `CHARGE_DEBUG`                            | Activates some debug utilities to Invoice Charges, only for testing purposes  | No       | `false`                                                                                                     |


## External services

1. Uses `MySQL` to store metrics data
2. Uses `Subscription` to charge
3. Uses `Recipients` to charge
4. Uses `Notification` to send email
5. Uses `Keychain` to block/unblock users      
6. Uses `Keycloak` to authenticate between services and also to authenticate endpoints
7. Uses `Pagar.me` to process payments and authorize card

## Keycloak

- Client ID: `billing`
- Service Account Enabled: **Yes**

### Roles

| Name                     | Description                                     | Composite | Composite Roles |
|--------------------------|-------------------------------------------------|-----------|-----------------|
| `user`                   | Role to read only                               | No        | -               |
| `admin`                  | Admin role with all roles, except for transfers | **Yes**   | `user`          |
| `transfer-letter-read`   | Read transfer letters                           | No        | -               |
| `transfer-letter-update` | Role to perform transfers on Transfer Letters   | No        | -               |

### Service Account Roles

| Client ID                     | Required Roles  | Description                                     |
|-------------------------------|-----------------|-------------------------------------------------|
| `gr1d-payments-recipients`    | `user`          | Uses to read recipient data                     |
| `gr1d-payments-subscriptions` | `user`          | Uses to read api, provider and tenant data      |

## Development

### Install

This project doesn't require installation

### Run

Run as java application in Eclipse or IntelliJ or Use docker-compose as mentioned above

### Run Tests

First configure some env variables

| ENV Name         | Description                | Required | Default Value   |
|------------------|----------------------------|----------|-----------------|
| `DB_URL`         | Address to MySQL database  | No       | `localhost`     |
| `DB_PORT`        | Port to MySQL database     | No       | `3306`          |
| `DB_NAME`        | Database schema            | No       | `gr1d_test`     |
| `DB_USER`        | MySQL database User        | No       | `root`          |
| `DB_PASSWORD`    | MySQL database password    | No       | `root`          |

Then run:

```
mvn test
```
