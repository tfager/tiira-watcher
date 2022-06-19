from os import environ
import firebase_admin
from firebase_admin import auth
from firebase_admin import credentials

# This script was part of tutorial here: https://www.padok.fr/en/blog/setup-application-gcp
# Modified to remove pyrebase (version conflicts) and we'd need to do login in React anyway

# ===== ADMIN APP FOR MANAGING USER ===== #

# App initialization
# To start in fish: set -x FIREBASE_CREDS_FILE (ls *firebase*.json)
creds_file = environ["FIREBASE_CREDS_FILE"]
cred = credentials.Certificate(creds_file)
firebase_admin_app = firebase_admin.initialize_app(cred)

# Signup => using firebase_admin
def signup():
    print("Sign up...")
    email = input("Enter email: ")
    password=input("Enter password: ")
    try:
        # More properties for user creation can be found on SDK Admin doc
        user = auth.create_user(email=email, password=password)

        # Create a custom token using userID after creation
        additional_claims = {'profile': "a-user-profile"}
        custom_token = auth.create_custom_token(user.uid, additional_claims)
    except Exception as e:
        print(e)
    return


signup()
