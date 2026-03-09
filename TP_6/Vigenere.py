from random import randint, choice
import string

class Vigenere :
  def __init__(self, key=None) :
    #compléter le corps du constructeur
    # La clé est une chaîne de caractères (mot)
    # Chaque lettre représente un décalage : A=0, B=1, ..., Z=25
    
    if key is None:
      # Générer une clé aléatoire de longueur 5 à 10
      key_length = randint(5, 10)
      self.key = ''.join(choice(string.ascii_uppercase) for _ in range(key_length))
    else:
      # Utiliser la clé fournie (convertie en majuscules)
      self.key = key.upper() 
  
  def encode(self, plain):
    cipher = ""

    #Cette fonction utilise la clé pour
    #faire un chifferement de Vigenère
    #
    #Le chifferement de `plain` est retourné dans `cipher`
    
    key_index = 0  # Index dans la clé (se répète cycliquement)
    
    for char in plain:
      if char.isalpha():
        # Obtenir le décalage de la lettre actuelle de la clé
        shift = ord(self.key[key_index % len(self.key)]) - ord('A')
        
        if char.isupper():
          # Chiffrer les majuscules
          cipher += chr((ord(char) - ord('A') + shift) % 26 + ord('A'))
        else:
          # Chiffrer les minuscules
          cipher += chr((ord(char) - ord('a') + shift) % 26 + ord('a'))
        
        # Avancer dans la clé seulement pour les lettres alphabétiques
        key_index += 1
      else:
        # Garder les caractères non-alphabétiques inchangés
        cipher += char

    return cipher

  def decode(self, cipher):
    plain = ""

    #Cette fonction utilise la clé pour
    #faire un déchiffrement de Vigenère
    #
    #Le déchiffrement de `cipher` est retourné dans `plain`
    
    key_index = 0  # Index dans la clé (se répète cycliquement)
    
    for char in cipher:
      if char.isalpha():
        # Obtenir le décalage de la lettre actuelle de la clé
        shift = ord(self.key[key_index % len(self.key)]) - ord('A')
        
        if char.isupper():
          # Déchiffrer les majuscules
          plain += chr((ord(char) - ord('A') - shift) % 26 + ord('A'))
        else:
          # Déchiffrer les minuscules
          plain += chr((ord(char) - ord('a') - shift) % 26 + ord('a'))
        
        # Avancer dans la clé seulement pour les lettres alphabétiques
        key_index += 1
      else:
        # Garder les caractères non-alphabétiques inchangés
        plain += char

    return plain

class DecryptVigenere :
  def chosen_cipher(self, vigenere, key_length=None):
    assert isinstance(vigenere, Vigenere)
    #déduire la clé en utilisant vigenere.decode()
    # Attaque par texte chiffré choisi:
    # On déchiffre un texte composé de 'A' répétés
    # Le résultat dévoile directement la clé inversée
    
    if key_length is None:
      # Si on ne connaît pas la longueur, utiliser 26 pour être sûr
      key_length = 26
    
    # Créer un texte chiffré de 'A' répétés
    chosen_cipher = 'A' * key_length
    
    # Déchiffrer ce texte
    decrypted = vigenere.decode(chosen_cipher)
    
    # Le texte déchiffré révèle l'inverse de la clé
    # Si la clé est "KEY", déchiffrer "AAA" donne "QAG" (inverse de K, E, Y)
    # Pour retrouver la clé, on doit inverser: A - decrypted
    key = ""
    for char in decrypted:
      if char.isalpha():
        # Calculer la lettre de la clé: shift = (A - decrypted) mod 26
        shift = (ord('A') - ord(char.upper())) % 26
        key += chr(shift + ord('A'))
    
    # Détecter la longueur réelle de la clé (pattern répétitif)
    actual_key = key
    for length in range(1, len(key) + 1):
      pattern = key[:length]
      if all(key[i] == pattern[i % length] for i in range(len(key))):
        actual_key = pattern
        break

    print("la clé de chiffrement est : {}".format(actual_key))
    return actual_key

  def chosen_plain(self, vigenere, key_length=None):
    assert isinstance(vigenere, Vigenere)
    #déduire la clé en utilisant vigenere.encode() mais sans utiliser vigenere.decode()
    # Attaque par texte clair choisi:
    # On chiffre un texte composé de 'A' répétés
    # Le résultat révèle directement la clé
    
    if key_length is None:
      # Si on ne connaît pas la longueur, utiliser 26 pour être sûr
      key_length = 26
    
    # Créer un texte en clair de 'A' répétés
    chosen_plain = 'A' * key_length
    
    # Chiffrer ce texte
    encrypted = vigenere.encode(chosen_plain)
    
    # Le texte chiffré révèle directement la clé
    # Si la clé est "KEY", chiffrer "AAA" donne "KEY"
    key = encrypted.upper()
    
    # Détecter la longueur réelle de la clé (pattern répétitif)
    actual_key = key
    for length in range(1, len(key) + 1):
      pattern = key[:length]
      if all(key[i] == pattern[i % length] for i in range(len(key))):
        actual_key = pattern
        break

    print("la clé de chiffrement est : {}".format(actual_key))
    return actual_key

  def known_plain(self, plain, cipher):
    #cipher est le message obtenu en chiffrant plain
    #déduire la clé à partir de plain et cipher
    # Attaque par texte clair connu:
    # On compare le texte clair et le texte chiffré pour déduire les décalages
    
    key = ""
    
    for i in range(min(len(plain), len(cipher))):
      if plain[i].isalpha() and cipher[i].isalpha():
        plain_char = plain[i].upper()
        cipher_char = cipher[i].upper()
        
        # Calculer le décalage: shift = (cipher - plain) mod 26
        shift = (ord(cipher_char) - ord(plain_char)) % 26
        key_char = chr(shift + ord('A'))
        key += key_char
    
    # Détecter la longueur réelle de la clé (pattern répétitif)
    actual_key = key
    if len(key) > 0:
      for length in range(1, len(key) + 1):
        pattern = key[:length]
        # Vérifier si le pattern se répète sur au moins 80% de la clé
        matches = sum(1 for i in range(len(key)) if key[i] == pattern[i % length])
        if matches >= len(key) * 0.8:
          actual_key = pattern
          break

    print("la clé de chiffrement est : {}".format(actual_key))
    return actual_key
