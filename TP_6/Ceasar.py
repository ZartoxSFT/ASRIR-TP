from random import randint

class Cesar :
  key = None
  def __init__(self, key=None) :
    if key is None:
      self.key = randint(1, 25)
    else:
      self.key = key

  def encode(self, plain):
    cipher = ""

    #Cette fonction utilise la clé pour
    #faire un chifferement de Cesar par décalage
    #
    #Le chifferement de `plain` est retourné dans `cipher`
    
    for char in plain:
      if char.isalpha():
        if char.isupper():
          cipher += chr((ord(char) - ord('A') + self.key) % 26 + ord('A'))
        else:
          cipher += chr((ord(char) - ord('a') + self.key) % 26 + ord('a'))
      else:
        
        cipher += char

    return cipher

  def decode(self, cipher):
    plain = ""

    #Cette fonction utilise la clé pour
    #faire un déchifferement de Cesar par décalage
    #
    #Le déchifferement de `cipher` est retourné dans `plain`
    
    for char in cipher:
      if char.isalpha():
        if char.isupper():
          plain += chr((ord(char) - ord('A') - self.key) % 26 + ord('A'))
        else:
          plain += chr((ord(char) - ord('a') - self.key) % 26 + ord('a'))
      else:
        
        plain += char

    return plain

def bruteforce_decrypt(cesar, cipher):
    assert isinstance(cesar, Cesar)

    print("Les possibilités de déchiffrement de {} sont :".format(cipher))

    #La suite de la fontion doit imprimer toutes les possibilités de déchiffrement ligne par ligne
    for test_key in range(1, 26):
        cesar_test = Cesar(test_key)
        decrypted = cesar_test.decode(cipher)
        print("Clé {}: {}".format(test_key, decrypted))

def chosen_cipher(cesar):
    assert isinstance(cesar, Cesar)
    #déduire la clé en utilisant cesar.decode()
    # Attaque par texte chiffré choisi:
    # On déchiffre 'A' pour voir quelle lettre on obtient
    # Si decode('A') donne 'X', alors la clé = (ord('A') - ord('X')) % 26
    decrypted = cesar.decode('A')
    key = (ord('A') - ord(decrypted)) % 26

    print("la clé de chiffrement est : {}".format(key))
    return key

def chosen_plain(cesar):
    assert isinstance(cesar, Cesar)
    #déduire la clé en utilisant cesar.encode() mais sans utiliser cesar.decode()
    # Attaque par texte clair choisi:
    # On chiffre 'A' pour voir quelle lettre on obtient
    # Si encode('A') donne 'X', alors la clé = (ord('X') - ord('A')) % 26
    encrypted = cesar.encode('A')
    key = (ord(encrypted) - ord('A')) % 26

    print("la clé de chiffrement est : {}".format(key))
    return key

def known_plain(plain, cipher):
    #cipher est le message obtenu en chiffrant plain
    #déduire la clé à partir de plain et cipher
    # Attaque par texte clair connu:
    # On compare le texte clair et le texte chiffré
    # On trouve le premier caractère alphabétique et on calcule la différence
    
    # Trouver le premier caractère alphabétique
    plain_char = None
    cipher_char = None
    
    for i in range(min(len(plain), len(cipher))):
        if plain[i].isalpha() and cipher[i].isalpha():
            plain_char = plain[i].upper()
            cipher_char = cipher[i].upper()
            break
    
    if plain_char and cipher_char:
        key = (ord(cipher_char) - ord(plain_char)) % 26
    else:
        key = 0

    print("la clé de chiffrement est : {}".format(key))
    return key

key = int(input("Veuillez saisir une clé de chiffrement ('-1' pour une clé aléatoire) :"))
cesar = Cesar() if key<0 else Cesar(key)

plain = input("Veuillez saisir un texte à chiffrer par la clé {}:".format(cesar.key))

print("Le chiffrement de {} par la clé {} est : {}".format(plain, cesar.key, cesar.encode(plain)))

cipher = input("Veuillez saisir un texte à déchiffrer par la clé {}:".format(cesar.key))

print("Le déchiffrement de {} par la clé {} est : {}".format(cipher, cesar.key, cesar.decode(cipher)))

# Ajoutez vos tests

print("\n" + "="*60)
print("TESTS DES ATTAQUES")
print("="*60)

# Test 1: Attaque par texte chiffré choisi
print("\n--- Test 1: Attaque par texte chiffré choisi (chosen_cipher) ---")
test_key = 7
cesar_test1 = Cesar(test_key)
print(f"Clé réelle: {test_key}")
found_key = chosen_cipher(cesar_test1)
print(f"Test réussi!" if found_key == test_key else f"Test échoué: trouvé {found_key}, attendu {test_key}")

# Test 2: Attaque par texte clair choisi
print("\n--- Test 2: Attaque par texte clair choisi (chosen_plain) ---")
test_key = 13
cesar_test2 = Cesar(test_key)
print(f"Clé réelle: {test_key}")
found_key = chosen_plain(cesar_test2)
print(f"Test réussi!" if found_key == test_key else f"Test échoué: trouvé {found_key}, attendu {test_key}")

# Test 3: Attaque par texte clair connu
print("\n--- Test 3: Attaque par texte clair connu (known_plain) ---")
test_key = 5
cesar_test3 = Cesar(test_key)
texte_clair = "HELLO WORLD"
texte_chiffre = cesar_test3.encode(texte_clair)
print(f"Clé réelle: {test_key}")
print(f"Texte clair: {texte_clair}")
print(f"Texte chiffré: {texte_chiffre}")
found_key = known_plain(texte_clair, texte_chiffre)
print(f"Test réussi!" if found_key == test_key else f"Test échoué: trouvé {found_key}, attendu {test_key}")

print("\n" + "="*60)
print("TESTS TERMINÉS")
print("="*60)