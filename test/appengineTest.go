package main

import (
	//uuid "code.google.com/p/go-uuid/uuid"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	//"strings"
)

func doOutput(url string, resp *http.Response, err error) {
	fmt.Printf("%s\n", url)
	if err != nil {
		fmt.Printf("\t%s", err)
	} else {
		defer resp.Body.Close()
		contents, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			fmt.Printf("\t%s", err)
		}
		fmt.Printf("\t%s: %s\n", resp.Status, string(contents))
	}
}

func testAnonView(base_url string, uniqueId string) {
	full_path := base_url + "/" + uniqueId
	resp, err := http.Get(full_path)
	doOutput(full_path, resp, err)
}

func testDirectedView(base_url string, keyId string, uniqueId string) {
	full_path := base_url + "/" + keyId + "/" + uniqueId
	resp, err := http.Get(full_path)
	doOutput(full_path, resp, err)
}

func testAnonRead(base_url string, uniqueId string) {
	full_path := base_url + "/api/read/" + uniqueId
	resp, err := http.Get(full_path)
	doOutput(full_path, resp, err)
}

func testDirectedRead(base_url string, keyId string, uniqueId string) {
	full_path := base_url + "/api/read/" + keyId + "/" + uniqueId
	resp, err := http.Get(full_path)
	doOutput(full_path, resp, err)
}

func testAnonWrite(base_url string, uniqueId string) {
	full_path := base_url + "/api/write/" + uniqueId
	resp, err := http.PostForm(full_path,
		url.Values{"MessageData": {"FooBar"}})
	doOutput(full_path, resp, err)
}

func testDirectedWrite(base_url string, keyId string, uniqueId string) {
	full_path := base_url + "/api/write/" + keyId + "/" + uniqueId
	resp, err := http.PostForm(full_path,
		url.Values{"MessageData": {"FooBarBaz"}})
	doOutput(full_path, resp, err)
}

func testGCMRegistration(base_url string, keyId string, regId string) {
	full_path := base_url + "/api/register/" + keyId + "/" + regId
	resp, err := http.Get(full_path)
	doOutput(full_path, resp, err)
}

func main() {
	keyId := "bc061370bfc1554c"
	base_url := os.Args[1]

	//uniqueId := uuid.New()
	//uniqueId = strings.Replace(uniqueId, "-", "", -1)

	uniqueId := "00000000000000000000000000000000"

	//fmt.Printf("%s/%s\n\n", os.Args[1], uniqueId)

	testGCMRegistration(base_url, keyId, "APA91bGPygr77n66RUWikNH_blEKceMSjrIE9mH2yNblnUZGTHTYZV6_gIqw1Btj3Tbv6xVjlDYiO2HROxeU9565ejP9h6AL_TvjeGNuOgfvZmodhS6zBjZejFm6W1u6RxPwJvqvqEPJQHgDIIwz6SZ7udehGZkzjQ")
	testAnonWrite(base_url, uniqueId)
	testDirectedWrite(base_url, keyId, uniqueId)
	testAnonView(base_url, uniqueId)
	testDirectedView(base_url, keyId, uniqueId)
	testAnonRead(base_url, uniqueId)
	testDirectedRead(base_url, keyId, uniqueId)
}
