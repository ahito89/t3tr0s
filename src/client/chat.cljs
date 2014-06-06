(ns client.chat
	(:require-macros [hiccups.core :as hiccups])
  (:require
		hiccups.runtime
    [client.socket :refer [socket]]
    [client.login :refer [get-username
                          get-color]]
    [cljs.reader :refer [read-string]]))

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml chat-html []
	[:div#inner-container
   [:div#chat
    [:div#chat-messages]
    [:div#chat-input
     [:input#msg {:type "text" :placeholder "Type to chat"}]
     [:input#submit {:type "submit" :value "Send"}]]]])

(hiccups/defhtml chat-msg-html
  [{:keys [user color msg]}]
  [:p.message
   [:span#user {:class (str "color-" color)}(str user ": ")]
   [:span.txt msg]])

(hiccups/defhtml chat-join-html
  [{:keys [user color]}]
  [:p.message
   [:span#user {:class (str "color-" color)}(str user " joined the lobby")]])

(hiccups/defhtml chat-leave-html
  [{:keys [user color]}]
  [:p.message
   [:span#user {:class (str "color-" color)}(str user " left the lobby")]])


; alias the jquery variable
(def $ js/$)

;;------------------------------------------------------------
;; Chat
;;-----------------------------------------------------------

(defn get-message
  "Gets the latest message in the input field"
  []
  (.val ($ "#msg")))

(defn clear-message!
  "Clear the current message in the input field"
  []
  (.val ($ "#msg") ""))

(defn send-message!
  "Sends a message to the chat"
  []
  (.emit @socket "chat-message" (get-message)))

(defn add-message!
  [msg]
  (if-let [html (get {"join"  chat-join-html
                      "leave" chat-leave-html
                      "msg"   chat-msg-html}
                     (:type msg))]
    (.append ($ "#chat-messages") (html msg))))

(defn submit-message!
  "adds a message, sends it and then removes it"
  []
  (let [msg (get-message)]
    (when-not (= msg "")
      (add-message! {:type "msg"
                     :user (get-username)
                     :color (get-color)
                     :msg msg})
      (send-message!)
      (clear-message!))))

(defn on-new-message
  "Called when we receive a chat message from the server."
  [data]
  (add-message! (read-string data)))

(defn on-start-game
  "Called when we receive the go-ahead from the server to start the game."
  []

  ; Join the "game" room to receive game-related messages.
  (.emit @socket "join-game")

  ; Navigate to the battle page.
  (aset js/location "hash" "#/battle-game")

  )

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  "Starts the chat page"
  []

  (.html ($ "#main-container") (chat-html))

  ;; Add listeners
  (.click ($ "#submit") submit-message!)
  (.keyup ($ "#msg") #(if (= (.-keyCode %) 13) (submit-message!)))

  ;; Join the "lobby" room.
  (.emit @socket "join-lobby")

  ;; Listen to chat updates.
  (.on @socket "new-message" on-new-message)

  (.on @socket "start-game" on-start-game)

  )

(defn cleanup
  []

  ;; Leave the "lobby" room.
  (.emit @socket "leave-lobby")

  ;; Ignore chat updates.
  (.removeListener @socket "new-message" on-new-message)

  ;; Ignore start game message.
  (.removeListener @socket "start-game" on-start-game)

  )