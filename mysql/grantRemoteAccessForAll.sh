#!/bin/bash

cd `dirname $0`

if [ -n "$1" ]; then
    root_pwd=$1
fi

# Check if MySQL root password is correct
while : ; do
    if [ -z "$root_pwd" ]; then
        read -s -p "Enter MySQL root password: " root_pwd; echo
    fi
    mysql -h localhost -u root -p$root_pwd --execute="SELECT 1;" > /dev/null
    if [[ $? -eq 0 ]]; then
        break
    else
        echo "Wrong password. Try again."
        root_pwd=""
    fi
done

echo "Enter domain, e.g. '192.168.2.' that shall be allowed remote access to this server, so for all hosts beginning with '192.168.2.'."
read -p "(Leave blank to grant access for all host names): " host; echo

# get new passwords from user
while : ; do
    read -s -p "Enter new admin password for MySQL user 'kassenadmin'@'$host%': " admin_pwd_1; echo
    read -s -p "Enter password again: " admin_pwd_2; echo
    if [[ "$admin_pwd_1" == "$admin_pwd_2" ]]; then
        break
    else
        echo "Passwords do not match. Try again."
    fi
done
admin_pwd=$admin_pwd_1
echo ""
while : ; do
    read -s -p "Enter new user password for MySQL user 'mitarbeiter'@'$host%': " mitar_pwd_1; echo
    read -s -p "Enter password again: " mitar_pwd_2; echo
    if [[ "$mitar_pwd_1" == "$mitar_pwd_2" ]]; then
        break
    else
        echo "Passwords do not match. Try again."
    fi
done
mitar_pwd=$mitar_pwd_1

sed "s/localhost/$host%/" grants.sql > grants_temp.sql

# create user admin and user mitarbeiter
# create DB, grant access rights, and create tables
echo ""
echo "Will now create the remote MySQL users..."
mysql --local-infile -h localhost -u root -p$root_pwd --execute="
#GRANT USAGE ON *.* TO 'kassenadmin'@'$host%';
#GRANT USAGE ON *.* TO 'mitarbeiter'@'$host%';
DROP USER IF EXISTS 'kassenadmin'@'$host%';
DROP USER IF EXISTS 'mitarbeiter'@'$host%';
CREATE USER 'kassenadmin'@'$host%' IDENTIFIED BY '$admin_pwd';
CREATE USER 'mitarbeiter'@'$host%' IDENTIFIED BY '$mitar_pwd';
SOURCE grants_temp.sql;
"
stats=$?

rm grants_temp.sql

echo ""
if [[ $stats -eq 0 ]]; then
    echo "Looks like the users were created without any errors!"
else
    echo "Seems there was a problem. Please check any error messages and try to solve the problem or ask for help."
fi

exit 0
