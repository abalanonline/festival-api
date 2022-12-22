# Copyright 2022 Aleksei Balan
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM maven:3.8-openjdk-11 AS build
COPY . ./
RUN mvn clean package

FROM debian:11-slim
RUN sed -i -e 's/bullseye main/bullseye main non-free/' /etc/apt/sources.list &&\
  apt-get update &&\
  apt-get install -y festival libttspico-utils openjdk-11-jre-headless &&\
  rm -rf /var/lib/apt/lists/*
COPY --from=build /target/festival-api-jar-with-dependencies.jar /festival-api.jar
EXPOSE 59125
CMD ["java", "-jar", "/festival-api.jar"]

# docker run -d --rm --name festival-api -p 59125:59125 festival-api
