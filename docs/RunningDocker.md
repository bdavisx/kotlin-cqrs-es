# Mongo

Created with: 

sudo docker run --name mongo-dev -it -v /opt/mongodb:/data/db -p 27017:27017 mongo:4.0-xenial

Should be able to run with 

sudo docker start mongo-dev



# Older - postgres stuff

sudo service docker start

sudo docker start local-postgres
sudo docker start pgadmin

sudo docker start local-postgres

***

create a custom network for easier connecting

sudo docker network create pg

start a postgres container

sudo docker run --name local-postgres -p 5432:5432 -e POSTGRES_PASSWORD=superSecretPassword -d --net pg -v /postgres-data:/var/lib/postgresql/data postgres:10.1

start pgAdmin container (can just use intellij)

sudo docker run -d -p 5050:5050 --name pgadmin --net pg thajeztah/pgadmin4

***

