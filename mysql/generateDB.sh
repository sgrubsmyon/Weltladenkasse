#!/bin/bash

show_help() {
    echo -e "Use \`-n\' to indicate that your MySQL needs no root password"
    echo -e "(works passwordless, but with sudo, as mariadb in Arch Linux)."
    echo -e "Without using \`-n\', you must specify the MySQL root password"
    echo -e "as first argument."
}

# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
# Special getopts variable:
OPTIND=1 # Reset in case getopts has been used previously in the shell.
# Our own variable:
nopwd=0

while getopts "hn" opt; do
    case "$opt" in
    h)  show_help
        exit 0
	;;
    n)  nopwd=1
        ;;
    esac
done

shift $((OPTIND-1))
[ "${1:-}" = "--" ] && shift

cd `dirname "$0"`

if [ -n "$1" ]; then
    root_pwd="$1"
fi

SQL_CHECK_USERS_EXIST="
SELECT EXISTS (SELECT User FROM mysql.user WHERE User = 'kassenadmin'),
EXISTS (SELECT User FROM mysql.user WHERE User = 'mitarbeiter'),
COUNT(SCHEMA_NAME) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'kasse';"

# Check if users already exist
echo "Checking if users/db already exist..."
while : ; do
    if [[ $nopwd -eq 0 ]]; then
        if [ -z "$root_pwd" ]; then
            read -s -p "Enter MySQL root password: " root_pwd; echo
        fi
	exists=$(mysql -h localhost -u root -p$root_pwd --skip-column-names --execute="$SQL_CHECK_USERS_EXIST")
    else
        exists=$(sudo mysql -u root --skip-column-names --execute="$SQL_CHECK_USERS_EXIST")
    fi
    if [[ $? -eq 0 ]]; then
        break
    else
        echo "Wrong password. Try again."
        root_pwd=""
    fi
done
admin_exists=${exists:0:1}
mitar_exists=${exists:2:1}
db_exists=${exists:4:1}

# print warning messages:
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
echo ""
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
command="
#GRANT USAGE ON *.* TO 'kassenadmin'@'localhost';
#GRANT USAGE ON *.* TO 'mitarbeiter'@'localhost';
DROP USER IF EXISTS 'kassenadmin'@'localhost';
DROP USER IF EXISTS 'mitarbeiter'@'localhost';
CREATE USER 'kassenadmin'@'localhost' IDENTIFIED BY '$admin_pwd';
CREATE USER 'mitarbeiter'@'localhost' IDENTIFIED BY '$mitar_pwd';
SOURCE generateDB.sql;
SOURCE fillWithInternalValues.sql;
SOURCE fillWithExampleData.sql;
"
if [[ $nopwd -eq 0 ]]; then
    mysql --local-infile -h localhost -u root -p$root_pwd --execute="$command"
else
    sudo mysql --local-infile -u root --execute="$command"
fi
stats=$?

echo ""
if [[ $stats -eq 0 ]]; then
    echo "Looks like the MySQL server was set up without any errors!"
    echo "You can start using the Kasse software by issuing \`java -jar Weltladenkasse_v1.4.0.jar'!"
else
    echo "Seems there was a problem. Please check any error messages and try to solve the problem or ask for help."
fi

exit 0
