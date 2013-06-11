package web_frontend

import (
	"appengine"
	"appengine/datastore"
	"appengine/urlfetch"
	pgparmor "code.google.com/p/go.crypto/openpgp/armor"
	"crypto/rand"
	"encoding/json"
	"fmt"
	"github.com/alexjlockwood/gcm"
	"github.com/gorilla/mux"
	"html/template"
	mRand "math/rand"
	"net/http"
)

type Response map[string]interface{}

func (r Response) String() (s string) {
	b, err := json.Marshal(r)
	if err != nil {
		s = ""
		return
	}
	s = string(b)
	return
}

type FrontPageData struct {
	Title          string
	BodyText       string
	AppStoreURL    string
	LogoPath       string
	Tagline        string
	Tagline2       string
	GetButtonText  string
	BitcoinAddress string
	FootNote       string
}

type Message struct {
	KeyId       string
	MessageData []byte
}

type GCMRegistration struct {
	KeyId    string
	GCMRegId string
}

func init() {
	r := mux.NewRouter()
	r.HandleFunc("/", IndexViewHandler)
	r.HandleFunc("/{guid:[0-9a-f]{32}}", AnonViewHandler)
	r.HandleFunc("/{keyid:[0-9a-f]{16}}/{guid:[0-9a-f]{32}}", DirectedViewHandler)
	r.HandleFunc("/api/read/{guid:[0-9a-f]{32}}", AnonReadHandler)
	r.HandleFunc("/api/write/{guid:[0-9a-f]{32}}", AnonWriteHandler)
	r.HandleFunc("/api/read/{keyid:[0-9a-f]{16}}/{guid:[0-9a-f]{32}}", DirectedReadHandler)
	r.HandleFunc("/api/write/{keyid:[0-9a-f]{16}}/{guid:[0-9a-f]{32}}", DirectedWriteHandler)
	r.HandleFunc("/api/register/{keyid:[0-9a-f]{16}}/{regid}", GCMRegistrationHandler)
	http.Handle("/", r)
}

func IndexViewHandler(w http.ResponseWriter, r *http.Request) {
	t, _ := template.ParseFiles("web_frontend/templates/index.html")
	s_LogoPath := "static/images/cryptweb_logo1_lg.png"
	s_Tagline := "The easy-to-use provably secure messaging app for your Android device. Best of all, it's free!"
	s_Tagline2 := "We promise we can't snoop on your messages and aren't working for the NSA."
	s_GetButtonText := "Get the CryptWeb app here!"
	s_BTCa := "Random_BTC_Address_String_Here_When_Done"
	s_FootNote := "Copyright 2012, CryptWeb.com"
	data := FrontPageData{"CryptWeb", "Body text yo", "http://play.google.com", s_LogoPath, s_Tagline, s_Tagline2, s_GetButtonText, s_BTCa, s_FootNote}
	t.Execute(w, data)
}

func AnonViewHandler(w http.ResponseWriter, r *http.Request) {
	AnonReadHandler(w, r)
}

func DirectedViewHandler(w http.ResponseWriter, r *http.Request) {
	DirectedReadHandler(w, r)
}

func AnonReadHandler(w http.ResponseWriter, r *http.Request) {
	c := appengine.NewContext(r)
	vars := mux.Vars(r)

	k := datastore.NewKey(c, "Message", vars["guid"], 0, nil)
	msg := new(Message)
	if err := datastore.Get(c, k, msg); err != nil {
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		armorOut, err := pgparmor.Encode(w, "PGP MESSAGE", map[string]string{"Version": "APG v2.0"})
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
		}
		rndBytes := make([]byte, mRand.Intn(3072)+1)
		rand.Read(rndBytes)
		armorOut.Write(rndBytes)
		armorOut.Close()
		//http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	datastore.Delete(c, k)
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprintf(w, string(msg.MessageData[:]))
}

func DirectedReadHandler(w http.ResponseWriter, r *http.Request) {
	c := appengine.NewContext(r)
	vars := mux.Vars(r)

	k := datastore.NewKey(c, "Message", vars["keyid"]+vars["guid"], 0, nil)
	msg := new(Message)
	if err := datastore.Get(c, k, msg); err != nil {
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		armorOut, err := pgparmor.Encode(w, "PGP MESSAGE", map[string]string{"Version": "APG v2.0"})
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
		}
		rndBytes := make([]byte, mRand.Intn(3072)+1)
		rand.Read(rndBytes)
		armorOut.Write(rndBytes)
		armorOut.Close()
		//http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprintf(w, string(msg.MessageData[:]))
}

func AnonWriteHandler(w http.ResponseWriter, r *http.Request) {
	c := appengine.NewContext(r)
	vars := mux.Vars(r)
	k := datastore.NewKey(c, "Message", vars["guid"], 0, nil)
	msg := Message{
		KeyId:       "",
		MessageData: []byte(r.FormValue("MessageData")),
	}
	_, err := datastore.Put(c, k, &msg)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, Response{"success": true})
}

func DirectedWriteHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	c := appengine.NewContext(r)
	k := datastore.NewKey(c, "Message", vars["keyid"]+vars["guid"], 0, nil)
	msg := Message{
		KeyId:       vars["keyid"],
		MessageData: []byte(r.FormValue("MessageData")),
	}
	_, err := datastore.Put(c, k, &msg)
	if err != nil {
		c.Errorf("Error: %s", err.Error())
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	gcmKey := datastore.NewKey(c, "GCMRegistration", vars["keyid"], 0, nil)
	gcmReg := new(GCMRegistration)
	gcmErr := datastore.Get(c, gcmKey, gcmReg)
	if gcmErr == nil {
		data := map[string]string{"keyid": vars["keyid"], "guid": vars["guid"]}
		gcmMsg := gcm.NewMessage(data, gcmReg.GCMRegId)
		c.Infof("GCM Message %s", gcmMsg.Data)
		client := urlfetch.Client(c)
		sender := &gcm.Sender{ApiKey: "AIzaSyCmUYKY7N3vcz7vwJk6fC7T1gyT7EyvLSU", Http: client}
		c.Infof("Sending GCM Message: %s, %s", gcmReg.KeyId, gcmReg.GCMRegId)
		sender.Send(gcmMsg, 2)
	} else {
		c.Errorf("Error sending GCM: %s", gcmErr.Error())
	}
	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, Response{"success": true})
}

func GCMRegistrationHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	c := appengine.NewContext(r)
	k := datastore.NewKey(c, "GCMRegistration", vars["keyid"], 0, nil)
	reg := GCMRegistration{
		KeyId:    vars["keyid"],
		GCMRegId: vars["regid"],
	}
	c.Infof("KeyID: %s, GCMRegId: %s", vars["keyid"], vars["regid"])
	_, err := datastore.Put(c, k, &reg)
	if err != nil {
		c.Errorf("Error: %s", err.Error())
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, Response{"success": true})
}
