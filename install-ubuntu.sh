#!/bin/bash

# Check if mysql is already installed via aptitude:
pm_check() {
    search_result=$(aptitude search \^mysql-server\$)
    search_result=${search_result:0:1}
    echo $search_result
    #return $search_result
}

# Check if mysql is in the $PATH, argument can be mysql root password:
path_check() {
    if [ -n "$1" ]; then
        mysqladmin -p$1 --version &> /dev/null
    else
        mysqladmin --version &> /dev/null
    fi
    mysql_in_path=$(echo $?)
    echo $mysql_in_path
    #return $mysql_in_path
}

# Check if mysql is executable, argument can be mysql root password:
run_check() {
    if [ -n "$1" ]; then
        mysql -h localhost -u root -p$1 --execute="quit"
    else
        mysql -h localhost -u root -p --execute="quit"
    fi
    mysql_runs_ok=$(echo $?)
    echo $mysql_runs_ok
}

print_problem_help() {
    echo -e "There seems to be a problem with your MySQL install."
    echo -e "Please try to install it again through your package manager."
    echo -e "Try running \`sudo apt-get remove mysql-server' and then \`sudo apt-get install mysql-server'."
}

echo -n "Checking if MySQL is installed via package manager... "
search_result=$(pm_check)
if [[ $search_result == "i" ]]; then echo "yes"; else echo "no"; fi
read -s -p "Enter MySQL root password: " root_pwd; echo
echo -n "Checking if MySQL admin tool is executable... "
mysqladmin_executable=$(path_check $root_pwd)
if [[ $mysqladmin_executable -eq 0 ]]; then echo "yes"; else echo "no"; fi
echo -n "Checking if MySQL works... "
mysql_works=$(run_check $root_pwd)
if [[ $mysql_works -eq 0 ]]; then echo "yes"; else echo "no"; fi

if [[ $search_result != "i" && $mysql_works -ne 0 ]]; then
    echo -e "MySQL seems to be not installed. Trying to install it now (need root privilieges)."
    sudo apt-get install mysql-server
    search_result=$(pm_check)
    read -s -p "Enter MySQL root password: " root_pwd; echo
    echo -n "Checking if MySQL works... "
    mysql_works=$(run_check $root_pwd)
    if [[ $search_result == "i" && $mysql_works -eq 0 ]]; then
        echo -e "MySQL is now successfully installed."
    else
        echo -e "There was an error during the MySQL installation \n(or you entered wrong root password)."
        print_problem_help
        exit 1
    fi
elif [[ $search_result != "i" && $mysql_works -eq 0 ]]; then
    echo -e "MySQL seems to have been installed manually, not via package manager.\nTrying to use this mysql install."
elif [[ $search_result == "i" && $mysql_works -eq 0 ]]; then
    echo -e "MySQL seems to be installed via package manager and working."
elif [[ $search_result == "i" && $mysql_works -ne 0 ]]; then
    echo -e "MySQL seems to be installed via package manager, but it is not working \n(or you entered wrong root password)."
    print_problem_help
    exit 1
fi

echo ""
`dirname $0`/mysql/generateDB.sh $root_pwd

exit 0

