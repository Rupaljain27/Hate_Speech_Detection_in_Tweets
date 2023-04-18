# Hate Speech Detection in Tweets

Author: Rupal Jain [jainrupal@arizona.edu](mailto:jainrupal@arizona.edu), Sarah Hyunju Song [hyunjusong@arizona.edu](mailto:hyunjusong@arizona.edu)

Date: April 16, 2023


## Running the program

You will need to set up an appropriate coding environment on whatever computer you expect to use for this assignment.
Minimally, you should install:

* [git](https://git-scm.com/downloads)
* [Java](https://www.java.com/en/)(8 or higher)
* [Maven](https://maven.apache.org/)
* Sign up for the Twitter developer portal and get the Twitter API (consumerKey, consumerSecret, accessToken, accessTokenSecret) and store it in config.properties file in the root folder

Firstly, copy this repository to your local directory by executing:

```
git clone https://github.com/Rupaljain27/Hate_Speech_Detection_in_Tweets.git
```

## Execution

Please run the below command in the terminal of the folder in which the repository has been cloned.

Compatibility:
Browser: Google Chrome, Microsoft Edge
OS: Windows, MAC

```
javac src/main/java/edu/arizona/cs/QueryEngine.java
```


## References

* https://developer.twitter.com/en/docs/twitter-api/getting-started/getting-access-to-the-twitter-api
* https://huggingface.co/pysentimiento/bertweet-hate-speech
* https://github.com/aymeam/Datasets-for-Hate-Speech-Detection
* https://twitter4j.org/configuration
* https://jar-download.com/artifacts/org.twitter4j/twitter4j-core/4.0.6/source-code/twitter4j/Paging.java
* https://twitter4j.org/oldjavadocs/2.2.5/twitter4j/Query.html
* https://developer.twitter.com/en/support/twitter-api/error-troubleshooting