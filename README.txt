CaixaPOS - Projeto Android (Starter)
===================================

O projeto dentro desta pasta é um **template mínimo** com:
- Jetpack Compose UI
- Exemplo de integração com Firebase (Auth + Firestore) - requer configuração
- Room dependências (pode ser usado localmente)

Importante:
1. Registre um app Android no Firebase Console (https://console.firebase.google.com/)
   - Use o package name: com.example.caixapos (ou altere no arquivo app/build.gradle e AndroidManifest.xml)
2. Faça o download do `google-services.json` gerado pelo Firebase e coloque em `app/` (substitua se existir).
3. Abra o projeto no Android Studio (preferível Arctic Fox ou superior) e permita o Gradle sincronizar.
4. Para snapshots/escuta em tempo real em produção, substitua a chamada na UI por listeners (snapshotListener).
5. Este projeto é um ponto de partida: adicione segurança (rules do Firestore), validação, tratamento de erros e autenticação adequada.

Arquivos principais:
- app/src/main/java/com/example/caixapos/MainActivity.kt  -> UI e integração Firestore básica
- app/build.gradle -> dependências (Firebase BOM incluido)
- README -> instruções

Boa sorte! Se quiser, eu já configuro o projeto com Hilt, DI, e split em múltiplos arquivos.
