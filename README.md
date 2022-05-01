# Iron Tunnel Chat [ITC]

### Analysis
Target users: people who don’t trust or can’t rely on any third-party authorities to provide the basis for symmetric key exchange and authentication, instead this will be done face to face using QR code scanning.
Goal: provide people with 100% trusted and secured channels for real-time communication such that no authorities will be able to eavesdrop on the conversation or impersonate one of the parties, because for example in chat apps like WhatsApp, the communication is secured between parties and WhatsApp servers, so the company is able to read conversations and impersonate one of the parties, such attack is called MITM (man in the middle).

### Use-Cases
1. Exchange of text or file messages between users in encrypted format
2. Exchange of text or file messages between users in plaintext format

### Functional Requirements
1. Create user accounts and optionally associate email addresses with them (used for password recovery)
2. Add peers (aka friend users) via QR code scan
3. Send encrypted and plaintext messages to peers
4. Generate encryption keys, and share them via QR code scan

### Non-Functional Requirements
1. Provide secure channel for communication between users
2. Provide Real time communication between users
3. Being easy to use by users that are not professionals in cryptography

### General Description
to use the app, the user will be required to create an account by providing only a username and a password (no personal information), after that he will need to add peers by meeting them in real life and scanning their personal account QR code. Users can send messages only to their peers, messages can be in plaintext or a key can be chosen to encrypt the message, but the other side wont be able to read the encrypted message unless they have scanned the key used for encryption, this will provide the authenticity of both communicating parties for both communicating parties and also create a trusted and secured channel between them, because keys were shared physically which is the most secure and reliable way for key exchange (although not so practical), in addition to that, encryption keys are generated in a secure manner which increases their resistance to any bruteforce (guessing) attacks, compared to keys (secret phrases) that could be created by users themselves.

### TODOs
ability to send files
ability to send images/voice/video messages
added anonimity: route traffic through TOR/L2P network
