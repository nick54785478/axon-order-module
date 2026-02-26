docker-compose up -d
#docker run --name local-mysql -p 3306:3306 --network-alias mysql -e MYSQL_ROOT_PASSWORD=root -d --rm mysql:8.0 
#docker run --name local-mysql -p 3306:3306 -v `pwd`/data:/var/lib/mysql -v `pwd`/mariadb.cnf:/etc/mysql/conf.d/mariadb.cnf -e MYSQL_ROOT_PASSWORD=root -d --rm mysql:8.0 

