#!/bin/bash

cd `dirname $0`

# Check if users already exist
echo "Checking if users/db already exist..."
while : ; do
    echo -n "Enter MySQL root password: "
    exists=$(mysql -h localhost -u root -p --skip-column-names --execute="
    SELECT EXISTS (SELECT User FROM mysql.user WHERE User = 'kassenadmin'), 
    EXISTS (SELECT User FROM mysql.user WHERE User = 'mitarbeiter'),
    COUNT(SCHEMA_NAME) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'kasse';"
    )
    if [[ $? -eq 0 ]]; then 
        break
    else
        echo "Wrong password. Try again."
    fi
done
admin_exists=${exists:0:1}
mitar_exists=${exists:2:1}
db_exists=${exists:4:1}

# print warning messages:
echo ""
if [[ $admin_exists -ne 0 ]]; then
    echo "WARN: MySQL user 'kassenadmin' already exists. Will DELETE AND RECREATE this user with new access privileges and password."
fi
if [[ $mitar_exists -ne 0 ]]; then
    echo "WARN: MySQL user 'mitarbeiter' already exists. Will DELETE AND RECREATE this user with new access privileges and password."
fi
if [[ $db_exists -ne 0 ]]; then
    echo "WARN: MySQL database 'kasse' already exists. ALL CONTENTS OF THE DATABASE WILL BE DELETED!!!"
fi
if [[ $admin_exists -ne 0 || $mitar_exists -ne 0 || $db_exists -ne 0 ]]; then
    echo -n "Are you sure you want to continue? (y/n) "
    read yesno
    if [ "$yesno" != "y" ] && [ "$yesno" != "Y" ] && [ "$yesno" != "yes" ] && [ "$yesno" != "Yes" ] ; then
	exit 1
    fi
fi

# get new passwords from user
echo ""
while : ; do
    read -s -p "Enter new admin password for MySQL user 'kassenadmin': " admin_pwd_1; echo
    read -s -p "Enter password again: " admin_pwd_2; echo
    if [[ "$admin_pwd_1" == "$admin_pwd_2" ]]; then 
        break
    else
        echo "Passwords do not match. Try again."
    fi
done
admin_pwd=$admin_pwd_1
while : ; do
    read -s -p "Enter new user password for MySQL user 'mitarbeiter': " mitar_pwd_1; echo
    read -s -p "Enter password again: " mitar_pwd_2; echo
    if [[ "$mitar_pwd_1" == "$mitar_pwd_2" ]]; then 
        break
    else
        echo "Passwords do not match. Try again."
    fi
done
mitar_pwd=$mitar_pwd_1

# create user admin and user mitarbeiter
# create DB, grant access rights, and create tables
echo ""
echo "Will now create the MySQL users and the database..."
echo -n "Enter MySQL root password: "
mysql --local-infile -h localhost -u root -p --execute="
GRANT USAGE ON *.* TO 'kassenadmin'@'localhost';
GRANT USAGE ON *.* TO 'mitarbeiter'@'localhost';
DROP USER 'kassenadmin'@'localhost';
DROP USER 'mitarbeiter'@'localhost';
CREATE USER 'kassenadmin'@'localhost' IDENTIFIED BY '$admin_pwd';
CREATE USER 'mitarbeiter'@'localhost' IDENTIFIED BY '$mitar_pwd';
SOURCE generateDB.sql;
SOURCE fillWithInternalValues.sql;
SOURCE fillWithExampleData.sql;
"
stats=$?

echo ""
if [[ $stats -eq 0 ]]; then
    echo "Looks like the MySQL server was set up without any errors!"
    echo "You can start using the Kasse software by issuing \`java -jar Weltladenkasse_v0.9.jar'!"
else
    echo "Seems there was a problem. Please check any error messages and try to solve the problem or ask for help."
fi

exit 0
