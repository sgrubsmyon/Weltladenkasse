#!/usr/bin/env python
# -*- coding: utf-8 -*-
from optparse import OptionParser
from getpass import getpass

# install parser
usage = "Usage: %prog   [OPTIONS]"
parser = OptionParser(usage)

parser.add_option("--host", type="string",
        default='localhost',
        dest="HOST",
        help="The hostname of the host hosting the MySQL DB. (Default: "
        "'localhost')")
parser.add_option("-p", "--pwd", type="string",
        default=None,
        dest="PWD",
        help="The admin(!) password of the MySQL DB. If not set, password is "
        "prompted for.")

# get parsed args
(options, args) = parser.parse_args()
pwd = options.PWD
if (pwd is None):
    pwd = getpass("Please enter admin(!) password of MySQL DB. ")

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector

def getDataFromDB(conn, SELECT, FROM, WHERE=None):
    """
    A generic wrapper function to fetch data from a MySQL DB.  Data is returned
    as a numpy matrix `res': first dimension is column, second dimension is
    row. So e.g. if you fetch certain values from the columns a and b, then
    res[0] returns the array with the values from a, res[1] has values from b.

    args:
        `conn': A connection to the DB created with mysql.connector.connect().
                Example:
                >>> conn = mysql.connector.connect(host="example.com",
                              user="usr", passwd="pwd", db="eno")

        `SELECT': String containing the list of columns you want
                (comma-separated)

        `FROM': String containing list of tables where
                the columns come from. Can contain multiple tables joined together.
                Examples: "mytable" or "mytable AS m LEFT JOIN othertable AS o"

        `WHERE': String containing the WHERE clause for filtering and/or other stuff,
                Examples: "" (fetch all rows), "WHERE a < 10." or "LIMIT 0,30" or "ORDER BY id".
                If None is given (default), then "LIMIT 0,30" is used.

    returns:
        `res': A numpy array with two dimensions: first column index, second row index.
    """
    res = np.array([])
    with conn:
        cursor = conn.cursor()
        if (WHERE is None):
            WHERE = 'LIMIT 0,30'
        query = "SELECT %s FROM %s %s" % (SELECT, FROM, WHERE)
        cursor.execute(query)
        things = cursor.fetchall()
        res = np.array(things)
        res = res.transpose()
        cursor.close()
    return res

conn = mysql.connector.connect(host=options.HOST, user="kassenadmin",
        password=pwd, db="kasse")
