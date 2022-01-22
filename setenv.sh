echo "usage: source $0"

# logback
export DOCKER_CONTAINER_ID=none
export DOCKER_HOSTNAME=none
export LOGGER_CC=nobody
export LOGGER_FROM=paul.little@akamesh.com
export LOGGER_TO=paul.little@akamesh.com
export SMTP_HOST=127.0.0.1
export SMTP_PORT=25

# AppTest
# sudo apt-get install pulseaudio-utils
export SPEAKER_MONITOR=$(pacmd list-sources | grep  stereo.monitor | grep -Po '<\K[^>]+') 
