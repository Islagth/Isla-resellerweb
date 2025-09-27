# Integrazione Enel API - Bitrix24 con Java Spring Boot

## Introduzione

Questo progetto fornisce un sistema di integrazione tra l'API Enel e Bitrix24 utilizzando Java con Spring Boot.  
Il sistema supporta:
- Autenticazione OAuth2 tramite JWT client assertion firmati RSA.
- Recupero e gestione dei lotti dati Enel in formato JSON e ZIP.
- Gestione lotti di blacklist forniti da Enel, inclusa conferma di processamento.
- Invio sicuro e affidabile dei dati lavorati a Bitrix24, con meccanismo di retry.
- API REST sicure con autenticazione token personalizzata.
- Creazione contatti su Bitrix24, utilizzando i dati ricevuti

---

## Caratteristiche principali

- **TokenService**: gestione avanzata del token OAuth2 con rinnovo automatico e firmature JWT.
- **OAuthService**: servizi di sicurezza per gestione OAuth, recupero, rinnovo.
- **TokenStorageService**: memorizzazione token.
- **DealService**: servizi dedicati per operazioni CRUD su deal in Bitrix24.
- **ContactService**: servizi dedicati per operazioni CRUD su contatti in Bitrix24.
- **BitrixService**: integrazione resiliente per invio contatti lavorati verso Bitrix24.
- **LottoService**: servizi REST per recupero, scaricamento e processamento di lotti dati.  
- **BlacklistService**: servizi REST per recupero, scaricamento e processamento di lotti blacklist.
- **EnelController**: controller REST centralizzato con validazione token Bearer e gestione endpoint.
- **BitrixController**: controller REST centralizzati per gestione endpoint API, con validazione token Bearer e flussi OAuth completi.
- **Test Completi**: suite di test unitari e di integrazione per garantire stabilità e sicurezza.

---

## Tecnologie

- Java 17+ con Spring Boot Framework
- Lombok per riduzione codice boilerplate
- Nimbus JOSE + JWT per gestione JWT firmati
- RestTemplate e WebClient per HTTP Client
- Mockito, JUnit 5 per testing unitario
- WireMock per test di integrazione simulata
- Maven per gestione delle dipendenze e build

---

## Struttura del progetto

Il progetto è organizzato principalmente sotto il package `src/main/java/com/example/enelbitrix24/`, con la seguente struttura:

<pre style="background:#f4f4f4; padding:10px;">
src/main/java/com/example/enelbitrix24/
 ├── config/          → Configurazioni applicative (EnelProperties, SecurityConfig, BitrixOAuthProperties, TokenResponse, TokenRecord, LeadScheduler, RestTamplateConfig)
 ├── controller/      → Controller REST (EnelController,BitrixController)
 ├── dto/             → Data Transfer Objects (LeadRequest,LeadResponse, ErrorResponse, LottoDTO, LottoBlacklistDTO, DealDTO, ContactDTO)
 ├── security/        → Gestione Token (TokenService, EnelClient, OAuthService, TokenStorageService)
 ├── service/         → Logica applicativa (BitrixService, LottoService, BlacklistService, DealService, ContactService)
 └── EnelBitrix24Application.java
    </pre>

---

## Descrizione Classi e File

### TokenService.java
Servizio che gestisce il recupero e rinnovo del token OAuth2 con autenticazione tramite client assertion JWT firmato con chiave RSA.  
Include:
- Parsing JWK nella fase di inizializzazione.  
- Metodo sincronizzato `getAccessToken()` per garantire rinnovo token sicuro.  
- Costruzione JWT client assertion e gestione chiamata REST per ottenere token.  
- Logging e gestione robusta degli errori.

### LottoDTO.java
DTO per il lotto con id_lotto, tipologia, data, contatti, anagrafiche e altri campi utili. Flag masked e slice_by_tag per funzioni particolari.

### LottoBlacklistDTO.java
DTO lotto blacklist con id, data creazione, dimensione e data conferma.

### ErrorResponse.java
DTO per risposte di errore con codice e messaggio.

### LeadRequest.java
DTO per invio dati lead con parametri di lavoro, campagna e contatti.

### LeadResponse.java
DTO risposta per gestione lead con esito successo, id lavoro e messaggio.

### LottoService.java
Servizio di gestione lotti con schedulazione recupero ogni minuto, download JSON/ZIP, e aggiornamento lista lotti.

### BlacklistService.java
Servizio simile ma dedicato alla blacklist con download ZIP e conferma processo lotto.

### BitrixService.java
Invio dati lavorati a Bitrix24 con retry automatico e gestione risposta.

### EnelController.java
Controller REST per API sotto `/api/enel-leads`, con autenticazione token e gestione completa di creazione contatti, lotti, blacklist e conferme.

### BitrixOAuthProperties.java
Classe Spring che carica proprietà configuration legate all’integrazione OAuth con Bitrix24 (clientId, clientSecret, redirectUri).

### TokenResponse.java
Classe che modella la risposta del server OAuth contenente access token e refresh token.

### TokenRecord.java
Modello per memorizzare un token con scadenza e info di refresh.

### BitrixController.java
Controller REST che gestisce gli endpoint per i deal e i contatti integrati in Bitrix24, con supporto a:
- Creazione, aggiornamento, recupero e cancellazione di deal e contatti
- Flusso completo OAuth con Bitrix24, gestione token e validazione sicurezza
- Deleghe ai servizi di business e sicurezza per le operazioni e il controllo accessi

### DealDTO.java
DTO che rappresenta un deal in Bitrix24 con molteplici campi quali titolo, stato, probabilità, dati economici, assegnatari, date, commenti e campi personalizzati.

### ContactDTO.java
DTO per i contatti Bitrix24 contenente informazioni anagrafiche, vari campi status, liste multifield (telefono, email, ecc) e metadati di creazione/editing.

### OAuthService.java
Servizio per l’acquisizione e rinnovo dei token OAuth da Bitrix24 tramite chiamate HTTP.

### TokenStorageService.java
Memorizza token OAuth attivi in memoria con relativa gestione scadenzia e refresh.

### DealService.java
Servizio che implementa operazioni CRUD per i deal su Bitrix24, usando RestTemplate per chiamate REST.

### ContactService.java
Gestisce la creazione, aggiornamento, lista e cancellazione dei contatti su Bitrix24, con gestione completa degli errori e logging.


---

## API Endpoint

Base URL: `/api/enel-leads`

| Metodo HTTP | Endpoint                                    | Descrizione                                    |
|-------------|--------------------------------------------|------------------------------------------------|
| POST        | /api/enel-leads/add-Deal                    | Aggiunge un nuovo Deal                           |
| PUT         | /api/enel-leads/update-Deal                 | Aggiorna un Deal esistente                       |
| GET         | /api/enel-leads/deal/{id}                   | Recupera un Deal per ID                           |
| POST        | /api/enel-leads/deal-list                   | Ottiene la lista di Deal                          |
| DELETE      | /api/enel-leads/delete-deal/{id}            | Elimina un Deal per ID                            |
| POST        | /api/enel-leads/idLotto/add-contact          | Aggiunge contatti da JSON                         |
| PUT         | /api/enel-leads/update-contact               | Aggiorna un contatto                              |
| GET         | /api/enel-leads/contact/{id}                 | Recupera contatto per ID                          |
| POST        | /api/enel-leads/contact-list                 | Ottiene lista contatti                            |
| DELETE      | /api/enel-leads/delete-contact/{id}          | Elimina contatto per ID                           |
| GET         | /api/enel-leads/oauth/authorize               | Avvia autorizzazione OAuth Bitrix                 |
| GET         | /api/enel-leads/oauth/callback                | Gestisce callback OAuth                            |
| POST        | /apienel-leads/creaContattoLavorato           | Crea un nuovo contatto lavorato                    |
| POST        | /apienel-leads/aggiungi                        | Aggiunge contatto per invio schedulato             |
| GET         | /apienel-leads/ultimi                          | Lista ultimi lotti disponibili                      |
| GET         | /apienel-leads/idLotto/json                    | Scarica lotto in formato JSON                       |
| GET         | /apienel-leads/idLotto/zip                     | Scarica lotto in formato ZIP (file binario)        |
| GET         | /apienel-leads/ultimiBlacklist                  | Lista ultimi lotti blacklist disponibili            |
| GET         | /apienel-leads/blacklist/idLotto/zip           | Scarica lotto blacklist in formato ZIP             |
| POST        | /apienel-leads/idconferma/{id}                  | Conferma processamento lotto blacklist             |


---

### POST `/`

**Content-Type:** application/json

**Descrizione:**  
Accetta un JSON con i seguenti elementi obbligatori:  

- `workedCode`: il numero di telefono del contatto lavorato  
- `workedDate`: data e ora inizio chiamata (yyyy/MM/dd HH:mm:ss)  
- `workedEndDate`: data e ora fine chiamata (yyyy/MM/dd HH:mm:ss)  
- `resultCode`: codice esito  
- `caller`: numero telefono chiamante  
- `workedType`: tipologia lavorato (O, E, V, B, C)  
- `campaignId`: id campagna  
- `contactId`: id anagrafica  

Elementi non obbligatori:  
- `chatHistory`: testo trascrizione chat  

**Esempio JSON richiesta:**
<pre style="background:#f4f4f4; padding:10px;">
{
"workedCode": "3423333333",
"worked_date": "2021/30/06 15:13:00",
"worked_end_date": "2021/30/06 15:16:24",
"resultCode": "500",
"caller": "3432222222",
"workedType": "O",
"campaignId": 123456,
"contactId": 456123
}
 </pre>
text

---

## Come eseguire

1. Clonare il repository:
git clone <repository-url>
cd <repository-folder>


2. Configurare `application.yml` con i parametri corretti.
3. Costruire il progetto con Maven:
mvn clean install


4. Avviare l’applicazione:
mvn spring-boot:run


5. Interagire con le API REST esposte.

---

## Testing

Eseguire i test con:
mvn test

Test unitari e di integrazione inclusi nel progetto assicurano la qualità e stabilità dell’applicazione.

---
