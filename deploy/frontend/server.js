const express = require("express");
const path = require("path");
const app = express();

// serve up production assets
app.use(express.static(path.join(__dirname)));

// let the react app handle any unknown routes
// serve up the index.html if express doesn't recognize the route
app.use((req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// if not in production use the port 5000
const PORT = process.env.PORT || 3000;

console.log("server started on port:", PORT);
app.listen(PORT);
