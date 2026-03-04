# Exercice 2

Q1. SELECT INBOX renvois la boîte INBOX en lecture et en écriture en affichant les information de la boite mail.


Q2. Les résultats changeront seulement si on envois, supprime ou modifie entre temps des mails.


Q3. Seen : Indique si un message a été lu par le client IMAP.
    Recent : Indique qu'un message est nouvellement arrivé dans la boîte aux lettres depuis la derniere connexion du client.


Q.4 1:* est une plage de message :
1 indique le premier message de la boîte
* indique le dernier message

Donc 1:* indique tous les message de la boîte allant du premier au dernier.

Pour récuperer tous le contenu du mail 5, on peut utiliser la commande :

FETCH 5 (BODY[])

Q.5 STORE 1 +FLAGS (\Deleted)
    * 1 FETCH (FLAGS (\SEEN\Deleted))
    OK STORE completed
    CLOSE
    * 1 EXPUNGE
    OK CLOSE completed

    
